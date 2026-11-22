package com.agrosentinel.agrosentinel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.agrosentinel.agrosentinel.dtos.AssetSpecificationRequest;
import com.agrosentinel.agrosentinel.dtos.SpecFieldRequest;
import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.AssetSpecification;
import com.agrosentinel.agrosentinel.models.Category;
import com.agrosentinel.agrosentinel.models.CategorySpecField;
import com.agrosentinel.agrosentinel.models.InventoryCodeStatus;
import com.agrosentinel.agrosentinel.models.SpecDataType;
import com.agrosentinel.agrosentinel.repositories.CategoryRepository;
import com.agrosentinel.agrosentinel.repositories.CategorySpecFieldRepository;

class AssetSpecificationServiceTest {
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final CategorySpecFieldRepository templateRepository = mock(CategorySpecFieldRepository.class);
    private final AssetSpecificationService service =
            new AssetSpecificationService(categoryRepository, templateRepository);

    @Test
    void templateFieldsTakeLabelUnitAndNumericType() {
        Asset asset = new Asset();
        List<CategorySpecField> template = List.of(
                field("power_hp", "Potencia", SpecDataType.NUMBER, "HP", true, 0),
                field("engine", "Motor", SpecDataType.TEXT, null, true, 1));

        service.applySpecifications(asset, List.of(
                new AssetSpecificationRequest("power_hp", "ignorada", "86,5", "kW"),
                new AssetSpecificationRequest("engine", null, "  Perkins A4248 ", null)), template);

        AssetSpecification power = asset.getSpecifications().get(0);
        assertEquals("Potencia", power.getLabel());
        assertEquals("HP", power.getUnit());
        assertEquals(new BigDecimal("86.5"), power.getValueNumber());
        assertNull(power.getValueText());
        assertEquals("Perkins A4248", asset.getSpecifications().get(1).getValueText());

        var responses = service.toResponses(asset, template);
        assertEquals("86.5 HP", responses.get(0).displayValue());
        assertFalse(responses.get(0).custom());
    }

    @Test
    void customFieldsKeepTheirLabelAndGoAfterTemplateFields() {
        Asset asset = new Asset();
        List<CategorySpecField> template = List.of(field("hitch", "Acople", SpecDataType.OPTION, null, true, 0));

        service.applySpecifications(asset, List.of(
                new AssetSpecificationRequest(null, "Presostato", "60–8 psi", null),
                new AssetSpecificationRequest("hitch", null, "Tres puntos", null)), template);

        assertEquals("hitch", asset.getSpecifications().get(0).getSpecKey());
        AssetSpecification custom = asset.getSpecifications().get(1);
        assertEquals("presostato", custom.getSpecKey());
        assertEquals("Presostato", custom.getLabel());
        assertTrue(service.toResponses(asset, template).get(1).custom());
    }

    @Test
    void updatesExistingRowsInPlaceAndRemovesEmptiedOnes() {
        Asset asset = new Asset();
        List<CategorySpecField> template = List.of(
                field("power_hp", "Potencia", SpecDataType.NUMBER, "HP", false, 0),
                field("engine", "Motor", SpecDataType.TEXT, null, false, 1));
        service.applySpecifications(asset, List.of(
                new AssetSpecificationRequest("power_hp", null, "80", null),
                new AssetSpecificationRequest("engine", null, "Perkins", null)), template);
        AssetSpecification originalPower = asset.getSpecifications().get(0);

        service.applySpecifications(asset, List.of(
                new AssetSpecificationRequest("power_hp", null, "86", null),
                new AssetSpecificationRequest("engine", null, "   ", null)), template);

        assertEquals(1, asset.getSpecifications().size());
        assertSame(originalPower, asset.getSpecifications().get(0));
        assertEquals(new BigDecimal("86"), originalPower.getValueNumber());
    }

    @Test
    void rejectsNonNumericValuesAndRepeatedKeys() {
        Asset asset = new Asset();
        List<CategorySpecField> template = List.of(field("power_hp", "Potencia", SpecDataType.NUMBER, "HP", false, 0));

        ResponseStatusException notNumber = assertThrows(ResponseStatusException.class,
                () -> service.applySpecifications(asset,
                        List.of(new AssetSpecificationRequest("power_hp", null, "86 HP a 2200", null)), template));
        assertTrue(notNumber.getReason().contains("Potencia"));

        assertThrows(ResponseStatusException.class, () -> service.applySpecifications(asset, List.of(
                new AssetSpecificationRequest(null, "Tiro", "1,8", null),
                new AssetSpecificationRequest("tiro", null, "2", null)), List.of()));
    }

    @Test
    void completenessCountsBaseDataAndRequiredTemplateFields() {
        Asset asset = new Asset();
        asset.setEquipmentType("Tractor agrícola");
        asset.setBrand("Massey Ferguson");
        asset.setModel("NMH 290");
        asset.setInventoryCode("25530003001");
        asset.setLocation("Granja Experimental");
        List<CategorySpecField> template = List.of(
                field("power_hp", "Potencia", SpecDataType.NUMBER, "HP", true, 0),
                field("engine", "Motor", SpecDataType.TEXT, null, false, 1));
        service.applySpecifications(asset,
                List.of(new AssetSpecificationRequest("power_hp", null, "86", null)), template);

        var completeness = service.completeness(asset, template);

        // 8 verificaciones (7 base + 1 obligatoria); faltan serial y fotografía.
        assertEquals(75, completeness.percent());
        assertEquals(List.of("Serial", "Fotografía"), completeness.missingFields());
    }

    @Test
    void replaceTemplateKeepsExistingFieldsByKeyAndDeletesRemovedOnes() {
        Category category = new Category();
        category.setId(4L);
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(category));
        CategorySpecField power = field("power_hp", "Potencia", SpecDataType.NUMBER, "HP", false, 0);
        CategorySpecField obsolete = field("color", "Color", SpecDataType.TEXT, null, false, 1);
        when(templateRepository.findAllByCategoryIdOrderBySortOrderAscIdAsc(4L))
                .thenReturn(new ArrayList<>(List.of(power, obsolete)));
        when(templateRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.replaceTemplate(4L, List.of(
                new SpecFieldRequest(null, "Acople", SpecDataType.OPTION, null, List.of("Tres puntos", " ", "Tiro"),
                        true, null),
                new SpecFieldRequest("power_hp", "Potencia máxima", SpecDataType.NUMBER, "HP", null, true, null)));

        assertEquals("acople", result.get(0).specKey());
        assertEquals(List.of("Tres puntos", "Tiro"), result.get(0).options());
        assertEquals("Potencia máxima", power.getLabel());
        assertEquals(1, power.getSortOrder());
        assertTrue(power.isRequired());
        verify(templateRepository).deleteAll(List.of(obsolete));
    }

    @Test
    void optionFieldsNeedOptions() {
        Category category = new Category();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        assertThrows(ResponseStatusException.class, () -> service.replaceTemplate(1L, List.of(
                new SpecFieldRequest(null, "Acople", SpecDataType.OPTION, null, List.of(), false, null))));
    }

    @Test
    void presetsHaveUniqueKeysAndOptionsWhereNeeded() {
        for (var preset : service.presets()) {
            long distinct = preset.fields().stream().map(field -> field.specKey()).distinct().count();
            assertEquals(preset.fields().size(), distinct, preset.key());
            preset.fields().stream()
                    .filter(field -> field.dataType() == SpecDataType.OPTION)
                    .forEach(field -> assertFalse(field.options().isEmpty(), field.specKey()));
        }
    }

    @Test
    void keysAreNormalizedFromLabels() {
        assertEquals("numero_de_motor", AssetSpecificationService.normalizeKey(" Número de motor "));
        assertEquals("diametro_succion_descarga", AssetSpecificationService.normalizeKey("Diámetro succión/descarga"));
    }

    @Test
    void inventoryCodeStatusFollowsTheCode() {
        assertEquals(InventoryCodeStatus.ASSIGNED,
                AssetService.resolveInventoryCodeStatus("2195001001", InventoryCodeStatus.NOT_PROVIDED));
        assertEquals(InventoryCodeStatus.PENDING, AssetService.resolveInventoryCodeStatus(null, null));
        assertEquals(InventoryCodeStatus.PENDING,
                AssetService.resolveInventoryCodeStatus(" ", InventoryCodeStatus.ASSIGNED));
        assertEquals(InventoryCodeStatus.NOT_PROVIDED,
                AssetService.resolveInventoryCodeStatus(null, InventoryCodeStatus.NOT_PROVIDED));
    }

    private static CategorySpecField field(String key, String label, SpecDataType type, String unit,
            boolean required, int order) {
        CategorySpecField field = new CategorySpecField();
        field.setSpecKey(key);
        field.setLabel(label);
        field.setDataType(type);
        field.setUnit(unit);
        field.setRequired(required);
        field.setSortOrder(order);
        if (type == SpecDataType.OPTION) {
            field.setOptions("Tres puntos\nTiro");
        }
        return field;
    }
}
