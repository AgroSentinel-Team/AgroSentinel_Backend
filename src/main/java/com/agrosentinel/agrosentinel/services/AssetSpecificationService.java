package com.agrosentinel.agrosentinel.services;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.AssetSpecificationRequest;
import com.agrosentinel.agrosentinel.dtos.AssetSpecificationResponse;
import com.agrosentinel.agrosentinel.dtos.SpecFieldRequest;
import com.agrosentinel.agrosentinel.dtos.SpecFieldResponse;
import com.agrosentinel.agrosentinel.dtos.SpecPresetResponse;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.AssetSpecification;
import com.agrosentinel.agrosentinel.models.Category;
import com.agrosentinel.agrosentinel.models.CategorySpecField;
import com.agrosentinel.agrosentinel.models.SpecDataType;
import com.agrosentinel.agrosentinel.repositories.CategoryRepository;
import com.agrosentinel.agrosentinel.repositories.CategorySpecFieldRepository;

import lombok.RequiredArgsConstructor;

/** Plantillas de especificaciones por categoría y datos técnicos de cada activo. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetSpecificationService {
    static final int MAX_TEMPLATE_FIELDS = 40;
    private static final int CUSTOM_SORT_OFFSET = 1000;

    private final CategoryRepository categoryRepository;
    private final CategorySpecFieldRepository templateRepository;

    public record Completeness(int percent, List<String> missingFields) {
    }

    // ---------- Plantillas ----------

    public List<SpecPresetResponse> presets() {
        return SpecPresets.all().stream()
                .map(preset -> {
                    List<SpecFieldResponse> fields = new ArrayList<>();
                    for (int index = 0; index < preset.fields().size(); index++) {
                        SpecPresets.Field field = preset.fields().get(index);
                        fields.add(new SpecFieldResponse(field.key(), field.label(), field.type(), field.unit(),
                                field.options(), field.required(), index, field.helpText()));
                    }
                    return new SpecPresetResponse(preset.key(), preset.name(), preset.description(), fields);
                })
                .toList();
    }

    public List<CategorySpecField> templateFor(Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        return templateRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(categoryId);
    }

    /** Todas las plantillas agrupadas por categoría (para listar muchos activos sin N+1). */
    public Map<Long, List<CategorySpecField>> templatesByCategory() {
        return templateRepository.findAll().stream()
                .sorted((left, right) -> Integer.compare(left.getSortOrder(), right.getSortOrder()))
                .collect(Collectors.groupingBy(field -> field.getCategory().getId()));
    }

    public List<SpecFieldResponse> getTemplate(Long categoryId) {
        findCategory(categoryId);
        return templateFor(categoryId).stream().map(this::toFieldResponse).toList();
    }

    /**
     * Reemplaza la plantilla de una categoría. Los campos que conservan su clave se actualizan
     * en su sitio; así no se pierden ni se duplican. Los datos ya registrados en los activos no
     * se borran: conservan su etiqueta y pasan a mostrarse como campos adicionales.
     */
    @Transactional
    public List<SpecFieldResponse> replaceTemplate(Long categoryId, List<SpecFieldRequest> requests) {
        Category category = findCategory(categoryId);
        if (requests.size() > MAX_TEMPLATE_FIELDS) {
            throw badRequest("Una plantilla admite como máximo " + MAX_TEMPLATE_FIELDS + " campos");
        }

        Map<String, CategorySpecField> existing = templateFor(categoryId).stream()
                .collect(Collectors.toMap(CategorySpecField::getSpecKey, Function.identity(), (a, b) -> a,
                        LinkedHashMap::new));
        Set<String> seenKeys = new HashSet<>();
        List<CategorySpecField> result = new ArrayList<>();

        for (int index = 0; index < requests.size(); index++) {
            SpecFieldRequest request = requests.get(index);
            String label = request.label().trim();
            String key = normalizeKey(isBlank(request.specKey()) ? label : request.specKey());
            if (key.isEmpty()) {
                throw badRequest("El campo \"" + label + "\" necesita una etiqueta con letras o números");
            }
            if (!seenKeys.add(key)) {
                throw badRequest("La plantilla tiene dos campos con la misma clave: " + key);
            }
            SpecDataType type = request.dataType() == null ? SpecDataType.TEXT : request.dataType();
            List<String> options = cleanOptions(request.options());
            if (type == SpecDataType.OPTION && options.isEmpty()) {
                throw badRequest("El campo \"" + label + "\" es de selección y necesita al menos una opción");
            }

            CategorySpecField field = existing.remove(key);
            if (field == null) {
                field = new CategorySpecField();
                field.setCategory(category);
                field.setSpecKey(key);
            }
            field.setLabel(label);
            field.setDataType(type);
            field.setUnit(blankToNull(request.unit()));
            field.setOptions(type == SpecDataType.OPTION ? String.join("\n", options) : null);
            field.setRequired(Boolean.TRUE.equals(request.required()));
            field.setSortOrder(index);
            field.setHelpText(blankToNull(request.helpText()));
            result.add(field);
        }

        templateRepository.deleteAll(new ArrayList<>(existing.values()));
        templateRepository.flush();
        return templateRepository.saveAll(result).stream().map(this::toFieldResponse).toList();
    }

    // ---------- Especificaciones del activo ----------

    /**
     * Sincroniza las especificaciones del activo con lo enviado: actualiza en su sitio las que ya
     * existen (la clave es única por activo), agrega las nuevas y quita las que vienen vacías o ya
     * no vienen.
     */
    public void applySpecifications(Asset asset, List<AssetSpecificationRequest> requests,
            List<CategorySpecField> template) {
        Map<String, CategorySpecField> templateByKey = template.stream()
                .collect(Collectors.toMap(CategorySpecField::getSpecKey, Function.identity(), (a, b) -> a));
        Map<String, AssetSpecification> current = new HashMap<>();
        for (AssetSpecification specification : asset.getSpecifications()) {
            current.put(specification.getSpecKey(), specification);
        }

        Set<String> keptKeys = new HashSet<>();
        int customIndex = 0;
        for (AssetSpecificationRequest request : requests) {
            if (request == null || isBlank(request.value())) {
                continue;
            }
            String rawKey = isBlank(request.specKey()) ? request.label() : request.specKey();
            String key = normalizeKey(rawKey);
            if (key.isEmpty()) {
                throw badRequest("Cada dato técnico adicional necesita un nombre");
            }
            if (!keptKeys.add(key)) {
                throw badRequest("El dato técnico \"" + rawKey + "\" está repetido");
            }

            CategorySpecField field = templateByKey.get(key);
            String label;
            String unit;
            SpecDataType type;
            int sortOrder;
            if (field != null) {
                label = field.getLabel();
                unit = field.getUnit();
                type = field.getDataType();
                sortOrder = field.getSortOrder();
            } else {
                AssetSpecification previous = current.get(key);
                label = !isBlank(request.label()) ? request.label().trim()
                        : previous != null ? previous.getLabel() : null;
                if (isBlank(label)) {
                    throw badRequest("El dato técnico adicional \"" + key + "\" necesita un nombre");
                }
                unit = blankToNull(request.unit());
                type = SpecDataType.TEXT;
                sortOrder = CUSTOM_SORT_OFFSET + customIndex++;
            }

            String value = request.value().trim();
            AssetSpecification specification = current.get(key);
            if (specification == null) {
                specification = new AssetSpecification();
                specification.setAsset(asset);
                specification.setSpecKey(key);
                asset.getSpecifications().add(specification);
            }
            specification.setLabel(label);
            specification.setUnit(unit);
            specification.setSortOrder(sortOrder);
            if (type == SpecDataType.NUMBER) {
                specification.setValueNumber(parseNumber(value, label));
                specification.setValueText(null);
            } else {
                specification.setValueText(value);
                specification.setValueNumber(null);
            }
        }

        asset.getSpecifications().removeIf(specification -> !keptKeys.contains(specification.getSpecKey()));
        asset.getSpecifications().sort((left, right) -> Integer.compare(left.getSortOrder(), right.getSortOrder()));
    }

    public List<AssetSpecificationResponse> toResponses(Asset asset, List<CategorySpecField> template) {
        Set<String> templateKeys = template.stream().map(CategorySpecField::getSpecKey).collect(Collectors.toSet());
        return asset.getSpecifications().stream()
                .map(specification -> {
                    String value = specification.getValueNumber() != null
                            ? formatNumber(specification.getValueNumber())
                            : specification.getValueText();
                    String display = specification.getUnit() == null || value == null
                            ? value
                            : value + " " + specification.getUnit();
                    return new AssetSpecificationResponse(specification.getSpecKey(), specification.getLabel(), value,
                            specification.getValueNumber(), specification.getUnit(), display,
                            !templateKeys.contains(specification.getSpecKey()));
                })
                .toList();
    }

    /**
     * Qué tan completa está la ficha: datos base de identificación más los campos marcados como
     * obligatorios en la plantilla de la categoría.
     */
    public Completeness completeness(Asset asset, List<CategorySpecField> template) {
        Map<String, Boolean> checks = new LinkedHashMap<>();
        checks.put("Tipo de equipo", !isBlank(asset.getEquipmentType()));
        checks.put("Marca", !isBlank(asset.getBrand()));
        checks.put("Modelo o referencia", !isBlank(asset.getModel()));
        checks.put("Serial", !isBlank(asset.getSerialNumber()));
        checks.put("Código de inventario", !isBlank(asset.getInventoryCode()));
        checks.put("Ubicación", !isBlank(asset.getLocation()));
        checks.put("Fotografía", !isBlank(asset.getImageUrl()));

        Set<String> filledKeys = asset.getSpecifications().stream()
                .filter(specification -> specification.getValueNumber() != null
                        || !isBlank(specification.getValueText()))
                .map(AssetSpecification::getSpecKey)
                .collect(Collectors.toSet());
        for (CategorySpecField field : template) {
            if (field.isRequired()) {
                checks.put(field.getLabel(), filledKeys.contains(field.getSpecKey()));
            }
        }

        List<String> missing = checks.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .toList();
        int percent = (int) Math.round(100.0 * (checks.size() - missing.size()) / checks.size());
        return new Completeness(percent, missing);
    }

    // ---------- Utilidades ----------

    static String normalizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String key = withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return key.length() > 60 ? key.substring(0, 60) : key;
    }

    static BigDecimal parseNumber(String value, String label) {
        String normalized = value.replace(" ", "");
        if (normalized.contains(",") && !normalized.contains(".")) {
            normalized = normalized.replace(',', '.');
        }
        try {
            BigDecimal number = new BigDecimal(normalized);
            if (number.abs().compareTo(new BigDecimal("99999999999")) > 0) {
                throw badRequest("El valor de \"" + label + "\" es demasiado grande");
            }
            return number;
        } catch (NumberFormatException exception) {
            throw badRequest("\"" + label + "\" debe ser un número (por ejemplo 86 o 2,5)");
        }
    }

    static String formatNumber(BigDecimal number) {
        return number.stripTrailingZeros().toPlainString();
    }

    private SpecFieldResponse toFieldResponse(CategorySpecField field) {
        List<String> options = field.getOptions() == null || field.getOptions().isBlank()
                ? List.of()
                : Arrays.stream(field.getOptions().split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        return new SpecFieldResponse(field.getSpecKey(), field.getLabel(), field.getDataType(), field.getUnit(),
                options, field.isRequired(), field.getSortOrder(), field.getHelpText());
    }

    private List<String> cleanOptions(List<String> options) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .filter(option -> option != null && !option.isBlank())
                .map(option -> option.trim().replace("\n", " "))
                .distinct()
                .toList();
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
