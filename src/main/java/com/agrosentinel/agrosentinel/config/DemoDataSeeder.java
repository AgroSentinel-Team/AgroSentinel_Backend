package com.agrosentinel.agrosentinel.config;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.agrosentinel.agrosentinel.models.Asset;
import com.agrosentinel.agrosentinel.models.Category;
import com.agrosentinel.agrosentinel.models.EquipmentFinalState;
import com.agrosentinel.agrosentinel.models.FailurePriority;
import com.agrosentinel.agrosentinel.models.FailureReport;
import com.agrosentinel.agrosentinel.models.FailureStatus;
import com.agrosentinel.agrosentinel.models.InventoryCodeStatus;
import com.agrosentinel.agrosentinel.models.InventoryItem;
import com.agrosentinel.agrosentinel.models.InventoryMovement;
import com.agrosentinel.agrosentinel.models.InventoryMovementType;
import com.agrosentinel.agrosentinel.models.MaintenanceHistory;
import com.agrosentinel.agrosentinel.models.MaintenanceHistoryStatus;
import com.agrosentinel.agrosentinel.models.MaintenanceAttentionIndex;
import com.agrosentinel.agrosentinel.models.MaintenanceType;
import com.agrosentinel.agrosentinel.models.OperationalStatus;
import com.agrosentinel.agrosentinel.models.PredictiveRecord;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenance;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceStatus;
import com.agrosentinel.agrosentinel.models.PreventiveMaintenanceTask;
import com.agrosentinel.agrosentinel.models.Usuario;
import com.agrosentinel.agrosentinel.models.WorkGroup;
import com.agrosentinel.agrosentinel.repositories.AssetRepository;
import com.agrosentinel.agrosentinel.repositories.CategoryRepository;
import com.agrosentinel.agrosentinel.repositories.FailureReportRepository;
import com.agrosentinel.agrosentinel.repositories.InventoryItemRepository;
import com.agrosentinel.agrosentinel.repositories.InventoryMovementRepository;
import com.agrosentinel.agrosentinel.repositories.MaintenanceHistoryRepository;
import com.agrosentinel.agrosentinel.repositories.PredictiveRecordRepository;
import com.agrosentinel.agrosentinel.repositories.PreventiveMaintenanceRepository;
import com.agrosentinel.agrosentinel.repositories.UsuarioRepository;
import com.agrosentinel.agrosentinel.repositories.WorkGroupRepository;

import lombok.RequiredArgsConstructor;

@Component
@Profile("demo-data")
@ConditionalOnProperty(name = "agrosentinel.demo-data.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String PASSWORD_DERIVATION_CONTEXT = "agrosentinel-demo-account-v1:";
    private static final String[] TECHNICIAN_NAMES = {
            "Juan David Rojas", "Camilo Andrés Pardo", "Santiago Muñoz", "Felipe Cárdenas", "Nicolás Vargas"
    };
    private static final String[] SUPERVISOR_NAMES = {
            "Laura Marcela Gómez", "Diana Carolina Salazar", "Andrés Felipe Torres", "María Camila Peña",
            "Jorge Esteban Molina"
    };
    private static final String[] OPERATOR_NAMES = {
            "Carlos Eduardo Ramírez", "Miguel Ángel Ortiz", "José Manuel Rincón", "Luis Fernando Castro",
            "Daniel Alejandro Suárez", "Óscar Mauricio Díaz", "Diego Alejandro Reyes", "Julián Andrés Herrera",
            "Kevin Santiago León", "Brayan Steven Cruz", "Héctor Fabio Medina", "Jhon Jairo Castaño",
            "Andrés Mauricio Silva", "Wilson Javier Mora", "Sebastián Ospina", "Edwin Alexander Gil",
            "Mateo Nicolás Parra", "Cristian Camilo Rojas", "Javier Orlando Peña", "Álvaro José Trujillo"
    };
    private static final String[] STOREKEEPER_NAMES = {
            "Mónica Patricia Arias", "Paola Andrea Lozano", "Ricardo Alonso Vera"
    };

    private final AssetRepository assetRepository;
    private final CategoryRepository categoryRepository;
    private final FailureReportRepository failureReportRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final MaintenanceHistoryRepository maintenanceHistoryRepository;
    private final PredictiveRecordRepository predictiveRecordRepository;
    private final PreventiveMaintenanceRepository preventiveMaintenanceRepository;
    private final UsuarioRepository usuarioRepository;
    private final WorkGroupRepository workGroupRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> generatedPasswords = new LinkedHashMap<>();

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${agrosentinel.demo-data.allow-remote:false}")
    private boolean allowRemote;

    @Value("${agrosentinel.demo-data.allowed-remote-host:}")
    private String allowedRemoteHost;

    @Value("${agrosentinel.demo-data.allowed-remote-database:}")
    private String allowedRemoteDatabase;

    @Value("${agrosentinel.demo-data.emit-credentials:false}")
    private boolean emitCredentials;

    @Value("${agrosentinel.demo-data.allow-weak-remote-passwords:false}")
    private boolean allowWeakRemotePasswords;

    @Value("${agrosentinel.demo-data.password-key:}")
    private String remotePasswordKey;

    private boolean remoteDatasource;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        requireAllowedDatasource(datasourceUrl, allowRemote, allowedRemoteHost, allowedRemoteDatabase);
        remoteDatasource = !isLocalDatasource(datasourceUrl);
        requireRemotePasswordKeyIfNeeded(datasourceUrl, remotePasswordKey, allowWeakRemotePasswords);

        if (!assetRepository.findAllByInventoryCodeIgnoreCase("25530003001").isEmpty()) {
            if (remoteDatasource && allowWeakRemotePasswords) {
                resetDemoAccountPasswords();
            }
            LOGGER.info("La carga de maquinaria ya existe; no se volvieron a insertar datos.");
            return;
        }
        if (hasExistingSeedData()) {
            throw new IllegalStateException(
                    "La carga de demostración solo se permite en una base sin datos de AgroSentinel.");
        }

        List<Usuario> technicians = upsertUsers("tecnico", "TECNICO", TECHNICIAN_NAMES);
        List<Usuario> supervisors = upsertUsers("supervisor", "SUPERVISOR", SUPERVISOR_NAMES);
        List<Usuario> operators = upsertUsers("operario", "OPERATIVO_CAMPO", OPERATOR_NAMES);
        List<Usuario> storekeepers = upsertUsers("almacenista", "ALMACENISTA", STOREKEEPER_NAMES);
        upsertUser("admin1@gmail.com", "Administrador de AgroSentinel", "ADMINISTRADOR", "admin1");

        Map<String, Category> categories = createCategories();
        List<WorkGroup> groups = createWorkGroups(technicians, supervisors, operators, storekeepers);
        List<Asset> assets = createAssets(categories, operators);
        createPreventiveHistory(assets, technicians);
        createFailureReports(assets, operators, technicians, supervisors, groups);
        createPredictiveRecords(assets);
        createInventory(groups, storekeepers);

        LOGGER.info(
                "Carga de demostración completada: {} usuarios de acceso, {} activos, {} grupos de trabajo, "
                        + "{} historiales preventivos, {} informes de fallas y {} registros predictivos.",
                34, assets.size(), groups.size(), assets.size(), 8, predictiveRecordRepository.count());
        registerCredentialOutput();
    }

    private boolean hasExistingSeedData() {
        return assetRepository.count() > 0
                || categoryRepository.count() > 0
                || failureReportRepository.count() > 0
                || inventoryItemRepository.count() > 0
                || inventoryMovementRepository.count() > 0
                || maintenanceHistoryRepository.count() > 0
                || predictiveRecordRepository.count() > 0
                || preventiveMaintenanceRepository.count() > 0
                || usuarioRepository.count() > 0
                || workGroupRepository.count() > 0;
    }

    private void resetDemoAccountPasswords() {
        resetAccountPassword("admin1@gmail.com", "admin1");
        resetAccountPasswords("tecnico", TECHNICIAN_NAMES.length);
        resetAccountPasswords("supervisor", SUPERVISOR_NAMES.length);
        resetAccountPasswords("operario", OPERATOR_NAMES.length);
        resetAccountPasswords("almacenista", STOREKEEPER_NAMES.length);
    }

    private void resetAccountPasswords(String prefix, int count) {
        for (int number = 1; number <= count; number++) {
            resetAccountPassword(prefix + number + "@gmail.com", prefix + number);
        }
    }

    private void resetAccountPassword(String email, String rawPassword) {
        Usuario user = usuarioRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalStateException(
                    "No se pudo actualizar la contraseña: falta la cuenta inicial " + email + ".");
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        usuarioRepository.save(user);
    }

    public static void requireRemotePasswordKeyIfNeeded(String datasourceUrl, String passwordKey) {
        requireRemotePasswordKeyIfNeeded(datasourceUrl, passwordKey, false);
    }

    public static void requireRemotePasswordKeyIfNeeded(
            String datasourceUrl, String passwordKey, boolean allowWeakRemotePasswords) {
        if (isLocalDatasource(datasourceUrl) || allowWeakRemotePasswords) {
            return;
        }
        if (passwordKey == null || passwordKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "La carga remota requiere una clave de credenciales de al menos 32 bytes.");
        }
    }

    public static void requireLocalDatasource(String datasourceUrl) {
        requireAllowedDatasource(datasourceUrl, false, "", "");
    }

    public static void requireAllowedDatasource(
            String datasourceUrl, boolean allowRemote, String allowedHost, String allowedDatabase) {
        if (isLocalDatasource(datasourceUrl)) {
            return;
        }
        if (!allowRemote || datasourceUrl == null || !datasourceUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException(
                    "La carga de datos de demostración solo se permite contra PostgreSQL local.");
        }

        URI uri;
        try {
            uri = URI.create(datasourceUrl.substring("jdbc:".length()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("La URL de base de datos no es válida para la carga remota.", exception);
        }
        String database = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
        if (allowedHost == null || allowedHost.isBlank()
                || allowedDatabase == null || allowedDatabase.isBlank()
                || !allowedHost.equalsIgnoreCase(uri.getHost())
                || !allowedDatabase.equals(database)) {
            throw new IllegalStateException(
                    "La carga remota requiere una coincidencia exacta de host y nombre de base autorizados.");
        }
    }

    private static boolean isLocalDatasource(String datasourceUrl) {
        if (datasourceUrl == null) {
            return false;
        }
        String normalizedUrl = datasourceUrl.toLowerCase(Locale.ROOT);
        return normalizedUrl.matches(
                "^jdbc:postgresql://(localhost|127\\.0\\.0\\.1|\\[::1\\])(:[0-9]+)?/.*$");
    }

    private List<Usuario> upsertUsers(String prefix, String role, String[] names) {
        List<Usuario> users = new ArrayList<>();
        for (int index = 0; index < names.length; index++) {
            int accountNumber = index + 1;
            users.add(upsertUser(
                    prefix + accountNumber + "@gmail.com",
                    names[index],
                    role,
                    prefix + accountNumber));
        }
        return users;
    }

    private Usuario upsertUser(String email, String name, String role, String rawPassword) {
        Usuario user = usuarioRepository.findByEmail(email);
        boolean created = user == null;
        if (user == null) {
            user = new Usuario();
            user.setEmail(email);
        }
        user.setNombre(name);
        user.setRol(role);
        if (!remoteDatasource || created || allowWeakRemotePasswords) {
            String password = accountPassword(
                    remoteDatasource, allowWeakRemotePasswords, email, rawPassword, remotePasswordKey);
            user.setPassword(passwordEncoder.encode(password));
            if (remoteDatasource && created && !allowWeakRemotePasswords) {
                generatedPasswords.put(email, password);
            }
        }
        user.setActivo(true);
        return usuarioRepository.save(user);
    }

    private String generatePassword(String email) {
        return derivePassword(email, remotePasswordKey);
    }

    static String accountPassword(
            boolean remoteDatasource,
            boolean allowWeakRemotePasswords,
            String email,
            String rawPassword,
            String passwordKey) {
        if (remoteDatasource && !allowWeakRemotePasswords) {
            return derivePassword(email, passwordKey);
        }
        return rawPassword;
    }

    static String derivePassword(String email, String passwordKey) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(passwordKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] passwordBytes = hmac.doFinal(
                    (PASSWORD_DERIVATION_CONTEXT + email.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(passwordBytes);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No fue posible derivar las credenciales de demostración.", exception);
        }
    }

    private void registerCredentialOutput() {
        if (!remoteDatasource || !emitCredentials || generatedPasswords.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                generatedPasswords.forEach((email, password) ->
                        System.out.println("AGRO_DEMO_CREDENTIAL\t" + email + "\t" + password));
            }
        });
    }

    private Map<String, Category> createCategories() {
        Map<String, Category> categories = new LinkedHashMap<>();
        addCategory(categories, "Tractor agrícola", "Tractores y maquinaria autopropulsada.", 90);
        addCategory(categories, "Implemento agrícola", "Implementos para preparación de suelos y labores de cultivo.", 120);
        addCategory(categories, "Remolque agrícola", "Remolques y planchones de transporte rural.", 90);
        addCategory(categories, "Equipo de bombeo", "Motobombas y equipos de suministro de agua.", 60);
        addCategory(categories, "Equipo de corte", "Cortamalezas, podadoras y equipos de corte.", 45);
        addCategory(categories, "Equipo de fumigación", "Equipos estacionarios y de aplicación de productos.", 45);
        addCategory(categories, "Equipo de siembra", "Sembradoras y equipos de trasplante.", 90);
        addCategory(categories, "Equipo de riego", "Carretes enrolladores y equipos de riego.", 90);
        addCategory(categories, "Equipo de compresión", "Compresores y equipos de aire.", 60);
        addCategory(categories, "Equipo motorizado", "Equipos motorizados portátiles y de preparación de suelo.", 45);
        return categories;
    }

    private void addCategory(Map<String, Category> categories, String name, String description, int frequencyDays) {
        Category category = categoryRepository.findAll().stream()
                .filter(existing -> name.equalsIgnoreCase(existing.getName()))
                .findFirst()
                .orElseGet(Category::new);
        category.setName(name);
        category.setDescription(description);
        category.setMaintenanceFrequencyDays(frequencyDays);
        category.setActive(true);
        categories.put(name, categoryRepository.save(category));
    }

    private List<WorkGroup> createWorkGroups(
            List<Usuario> technicians,
            List<Usuario> supervisors,
            List<Usuario> operators,
            List<Usuario> storekeepers) {
        List<WorkGroup> groups = new ArrayList<>();
        for (int index = 0; index < supervisors.size(); index++) {
            WorkGroup group = new WorkGroup();
            group.setName("Cuadrilla de campo " + (index + 1));
            group.setDescription("Equipo de mantenimiento y operación de maquinaria agrícola, zona "
                    + (index + 1) + ".");
            group.setLeader(supervisors.get(index));
            group.getMembers().add(supervisors.get(index));
            group.getMembers().add(technicians.get(index));
            group.getMembers().add(storekeepers.get(index % storekeepers.size()));
            for (int operatorIndex = index * 4; operatorIndex < index * 4 + 4; operatorIndex++) {
                group.getMembers().add(operators.get(operatorIndex));
            }
            groups.add(workGroupRepository.save(group));
        }
        return groups;
    }

    private List<Asset> createAssets(Map<String, Category> categories, List<Usuario> operators) {
        List<AssetDefinition> definitions = List.of(
                new AssetDefinition("Tractor agrícola Massey Ferguson 290", "Massey Ferguson", "NMH 290",
                        null, "25530003001", "Tractor agrícola", true, OperationalStatus.OPERATIONAL,
                        "Motor Perkins A4248 diésel de 4 cilindros, 86 HP. Requiere atención a las llantas delanteras."),
                new AssetDefinition("Tractor agrícola New Holland ZBC834", "New Holland", "ZBC834",
                        null, null, "Tractor agrícola", true, OperationalStatus.OUT_OF_SERVICE,
                        "Motor número 6059574. El informe registra falla grave y recomienda documentar la evaluación económica de recuperación."),
                new AssetDefinition("Transplantadora de arroz Kubota SPV-8C", "Kubota", "SPV-8C",
                        null, null, "Equipo de siembra", true, OperationalStatus.OPERATIONAL,
                        "Año 2020, potencia reportada de 22 HP."),
                new AssetDefinition("Remolque agrícola de un eje", null, "500 kg",
                        null, "263000003001", "Remolque agrícola", false, OperationalStatus.IN_REPAIR,
                        "Capacidad 500 kg. El informe indica ausencia de llantas y reparación pendiente de bisagras."),
                new AssetDefinition("Remolque agrícola Moragro", "Moragro", "6 toneladas",
                        null, "26300002001", "Remolque agrícola", false, OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS,
                        "Plataforma metálica de 4 x 2 m; el informe recomienda mantenimiento."),
                new AssetDefinition("Cultivadora MANAGRO CLT", "MANAGRO", "CLT, 2 surcos",
                        null, "22860031001", "Implemento agrícola", false, OperationalStatus.OPERATIONAL,
                        "Cultivadora de dos surcos; requiere mantenimiento periódico."),
                new AssetDefinition("Rastra transportable INTERA", "INTERA", "55x20",
                        null, "22860032001", "Implemento agrícola", false, OperationalStatus.OPERATIONAL,
                        "20 discos de 24 pulgadas y sistema de cilindro hidráulico."),
                new AssetDefinition("Rastrillo pulidor INTERAGRO", "INTERAGRO", null,
                        null, "22860033001", "Implemento agrícola", false, OperationalStatus.IN_REPAIR,
                        "El resumen del informe lo registra fuera de servicio, recuperable mediante mantenimiento."),
                new AssetDefinition("Motobomba diésel KAMA", "KAMA", "10 HP, 3 pulgadas",
                        null, "242000014002", "Equipo de bombeo", true, OperationalStatus.OPERATIONAL,
                        "Equipo de alta presión usado para suministro de agua desde reservorios."),
                new AssetDefinition("Cortamaleza INTERAGRO CM 72", "INTERAGRO", "CM 72",
                        null, "24410003001", "Equipo de corte", true, OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS,
                        "El informe recomienda mantenimiento preventivo."),
                new AssetDefinition("Pala niveladora Enciso", "Enciso", null,
                        null, "24470012001", "Implemento agrícola", false, OperationalStatus.OPERATIONAL,
                        "Implemento de nivelación reportado en el inventario de maquinaria."),
                new AssetDefinition("Carrete enrollador Bauer Sport Rain", "Bauer Austria", "Sport Rain",
                        null, "22860029001", "Equipo de riego", false, OperationalStatus.OUT_OF_SERVICE,
                        "El informe recomienda evaluar su recuperación antes de asignarlo a operación."),
                new AssetDefinition("Podadora de altura Husqvarna 525P", "Husqvarna", "525P",
                        "20203200072", "228600105001", "Equipo de corte", true, OperationalStatus.OPERATIONAL,
                        "El informe recomienda cambio de cadena y bujía."),
                new AssetDefinition("Sembradora abonadora al voleo Veloz", "Veloz", null,
                        null, "22860024001", "Equipo de siembra", false, OperationalStatus.OPERATIONAL,
                        "Sembradora abonadora registrada en el resumen de maquinaria."),
                new AssetDefinition("Fumigadora estacionaria Honda ES30", "Honda", "ES30 / 5005",
                        null, "216800013001", "Equipo de fumigación", true, OperationalStatus.OPERATIONAL,
                        "Equipo estacionario reportado en la ficha técnica."),
                new AssetDefinition("Fumigadora de espalda Olympia NP 250", "Olympia", "NP 250",
                        null, "2163002003", "Equipo de fumigación", true, OperationalStatus.OBSOLETE,
                        "El informe la registra fuera de servicio y recomienda evaluar su baja."),
                new AssetDefinition("Fumigadora de lanza Husqvarna 321S25", "Husqvarna", "321S25",
                        "20213600133", "216800020004", "Equipo de fumigación", true, OperationalStatus.OPERATIONAL,
                        "El informe recomienda mantenimiento preventivo."),
                new AssetDefinition("Arado cincel vibratorio ALCE", "ALCE", "AE-6",
                        null, "25540001001", "Implemento agrícola", false, OperationalStatus.OPERATIONAL,
                        "Identificación adicional IRS54420. Requiere cambio de cuchillas o puntas y revisión de brazos y acople."),
                new AssetDefinition("Arado cincel rígido INTERAGRO", "INTERAGRO", "3 vástagos",
                        "87091706", "25540002001", "Implemento agrícola", false, OperationalStatus.OPERATIONAL,
                        "El informe registra el equipo en buen estado y funcional."),
                new AssetDefinition("Planchón arrocero Monagro", "Monagro", "4 toneladas",
                        null, "26310001001", "Remolque agrícola", false, OperationalStatus.OUT_OF_SERVICE,
                        "El informe lo registra no operativo y recomienda revisar su recuperación."),
                new AssetDefinition("Motocompresor Juzzi", "Juzzi", "2 HP, 1.800 rpm",
                        null, "22860027001", "Equipo de compresión", true, OperationalStatus.OPERATIONAL,
                        "Motor eléctrico de 2 HP y tanque con válvula de drenaje; el informe lo registra funcional."),
                new AssetDefinition("Desbrozador DB 200", null, "DB 200",
                        "ID 54410", "25550001001", "Equipo motorizado", true, OperationalStatus.OUT_OF_SERVICE,
                        "Ancho 2 m. El informe indica que está fuera de servicio y requiere intervención."),
                new AssetDefinition("Sembradora de grano fino Montana SGF17D", "Montana", "SGF17D",
                        "SGF1722019579", "228600220002", "Equipo de siembra", false, OperationalStatus.OPERATIONAL,
                        "Sembradora de 17 filas; el informe la registra en buen estado y funcional."),
                new AssetDefinition("Microniveladora multicuchillas Soto NS-5", "Soto", "NS-5",
                        "NS-5 / 244-22", "228600221009", "Implemento agrícola", false, OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS,
                        "El informe recomienda reemplazar los neumáticos."),
                new AssetDefinition("Caballoneador Taipa Soto", "Soto", "TA-6, 6 discos",
                        "TA-6 / 235-22", "228600222004", "Implemento agrícola", false, OperationalStatus.OPERATIONAL,
                        "El informe recomienda inspección preventiva periódica."),
                new AssetDefinition("Sembradora de arroz Semeato", "Semeato", "13-46",
                        null, "22860023002", "Equipo de siembra", false, OperationalStatus.OUT_OF_SERVICE,
                        "El informe la registra fuera de servicio y no funcional; recomienda evaluación técnica y económica."),
                new AssetDefinition("Sembradora John Deere 8250", "John Deere", "8250",
                        null, "22860025001", "Equipo de siembra", false, OperationalStatus.OUT_OF_SERVICE,
                        "Equipo para granos y fertilizantes, 13 filas. El informe la registra fuera de servicio."),
                new AssetDefinition("Motosierra STIHL MS 310-3", "STIHL", "MS 310-3",
                        null, null, "Equipo motorizado", true, OperationalStatus.OPERATIONAL,
                        "El informe registra buen estado funcional. El código 228600105001 también aparece en otra ficha y requiere verificación."),
                new AssetDefinition("Ahoyadora EFCO TR1551", "EFCO", "TR1551",
                        null, "228600075001", "Equipo motorizado", true, OperationalStatus.OPERATIONAL,
                        "Barreno de 50 cm de longitud y 20 cm de diámetro."),
                new AssetDefinition("Desgranadora de maíz CAP12", null, "CAP12",
                        null, "228600074001", "Implemento agrícola", false, OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS,
                        "Capacidad reportada de 1.500 kg/h y acople a toma de fuerza; requiere mantenimiento."),
                new AssetDefinition("Plantadora de cuatro surcos Montana PLA4S", "Montana", "PLA4S",
                        null, "228600064001", "Equipo de siembra", false, OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS,
                        "Equipo para fríjol, algodón y granos gruesos; el informe recomienda mantenimiento."),
                new AssetDefinition("Aguilón de fumigación de 7 metros", null, "7 m / 32 boquillas",
                        null, "2195001001", "Equipo de fumigación", false, OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS,
                        "El informe indica 32 boquillas, mangueras en buen estado y mantenimiento requerido."),
                new AssetDefinition("Guadaña Maruyama AE420", "Maruyama", "AE420",
                        null, "216700015001", "Equipo motorizado", true, OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS,
                        "Cilindrada 41,5 cc; el informe recomienda cambio de zapatas y bujía."),
                new AssetDefinition("Guadaña Husqvarna 143R-II", "Husqvarna", "143R-II",
                        null, "216700017004", "Equipo motorizado", true, OperationalStatus.OPERATIONAL,
                        "El informe registra buen estado y recomienda mantenimiento básico."),
                new AssetDefinition("Guadaña Husqvarna 143R-II", "Husqvarna", "143R-II",
                        null, "216700017005", "Equipo motorizado", true, OperationalStatus.OPERATIONAL,
                        "El informe registra buen estado funcional y mantenimiento preventivo."),
                new AssetDefinition("Guadaña Husqvarna 143R-II", "Husqvarna", "143R-II",
                        null, "216700017006", "Equipo motorizado", true, OperationalStatus.OPERATIONAL_WITH_RESTRICTIONS,
                        "El informe indica que requiere intervención del sistema para funcionar."),
                new AssetDefinition("Fumigadora de lanza Husqvarna 321S25", "Husqvarna", "321S25",
                        "2019", "216800020005", "Equipo de fumigación", true, OperationalStatus.OPERATIONAL,
                        "El informe recomienda cambio de aceite y mantenimiento preventivo."),
                new AssetDefinition("Fumigadora de lanza Husqvarna 321S25", "Husqvarna", "321S25",
                        "20194501630", "216800020006", "Equipo de fumigación", true, OperationalStatus.OUT_OF_SERVICE,
                        "El informe registra buen estado general, pero fuera de servicio."),
                new AssetDefinition("Fumigadora de lanza Mitsubishi TB26", "Mitsubishi", "TB26",
                        null, null, "Equipo de fumigación", true, OperationalStatus.OPERATIONAL,
                        "El informe no suministra serial ni código de inventario y la registra funcional."),
                new AssetDefinition("Fumigadora de polvo Husqvarna 362D28", "Husqvarna", "362D28",
                        "20210900447", "216800019003", "Equipo de fumigación", true, OperationalStatus.OPERATIONAL,
                        "Referencia adicional 966708201; ficha técnica en buen estado funcional."),
                new AssetDefinition("Fumigadora de polvo Husqvarna 362D28", "Husqvarna", "362D28",
                        "20210900447", "216800019004", "Equipo de fumigación", true, OperationalStatus.OPERATIONAL,
                        "Referencia adicional 966708201; ficha técnica en buen estado funcional."),
                new AssetDefinition("Guadaña Husqvarna 143R-II fuera de servicio", "Husqvarna", "143R-II",
                        null, "216700014001", "Equipo motorizado", true, OperationalStatus.OUT_OF_SERVICE,
                        "El informe recomienda evaluación técnica y económica para determinar su recuperación."),
                new AssetDefinition("Guadaña Enermax GX35-4T", "Enermax", "GX35-4T",
                        null, "216700013001", "Equipo motorizado", true, OperationalStatus.OUT_OF_SERVICE,
                        "Cilindrada 35 cc y referencia adicional 65LT; requiere diagnóstico integral."),
                new AssetDefinition("Guadaña Husqvarna 142 rígida", "Husqvarna", "142 rígida",
                        null, "216700010001", "Equipo motorizado", true, OperationalStatus.OUT_OF_SERVICE,
                        "Potencia 3 HP, cilindrada 42 cc; el informe recomienda revisar motor y encendido."));

        List<Asset> assets = new ArrayList<>();
        for (int index = 0; index < definitions.size(); index++) {
            AssetDefinition definition = definitions.get(index);
            Asset asset = new Asset();
            asset.setName(definition.name());
            asset.setBrand(definition.brand());
            asset.setModel(definition.model());
            asset.setSerialNumber(definition.serialNumber());
            asset.setInventoryCode(definition.inventoryCode());
            asset.setEquipmentType(definition.category());
            asset.setManufactureYear("SPV-8C".equals(definition.model()) ? 2020 : null);
            asset.setExtraSpecifications(definition.reportNotes());
            asset.setCategory(categories.get(definition.category()));
            asset.setResponsible(operators.get(index % operators.size()));
            asset.setInventoryCodeStatus(definition.inventoryCode() == null
                    ? InventoryCodeStatus.PENDING
                    : InventoryCodeStatus.ASSIGNED);
            asset.setLocation("Granja Experimental - Universidad Surcolombiana");
            asset.setHasMotor(definition.hasMotor());
            asset.setOperationalStatus(definition.status());
            asset.setImageUrl(imageFor(definition.category()));
            assets.add(assetRepository.save(asset));
        }
        return assets;
    }

    private String imageFor(String category) {
        return switch (category) {
            case "Tractor agrícola" -> "/images/demo-assets/tractor.svg";
            case "Equipo de fumigación" -> "/images/demo-assets/sprayer.svg";
            case "Equipo de corte" -> "/images/demo-assets/mower.svg";
            case "Equipo motorizado" -> "/images/demo-assets/mower.svg";
            case "Equipo de bombeo", "Equipo de riego" -> "/images/demo-assets/pump.svg";
            default -> "/images/demo-assets/implement.svg";
        };
    }

    private void createPreventiveHistory(List<Asset> assets, List<Usuario> technicians) {
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < assets.size(); index++) {
            Asset asset = assets.get(index);
            Usuario technician = technicians.get(index % technicians.size());
            LocalDateTime executedAt = now.minusDays(15L + index * 5L);

            PreventiveMaintenance preventive = new PreventiveMaintenance();
            preventive.setAsset(asset);
            preventive.setTechnician(technician);
            preventive.setExecutedBy(technician);
            preventive.setScheduledAt(executedAt);
            preventive.setExecutedAt(executedAt.plusHours(2));
            preventive.setStatus(PreventiveMaintenanceStatus.COMPLETED);
            preventive.setAttentionIndex(index % 5 == 0 ? MaintenanceAttentionIndex.HIGH : MaintenanceAttentionIndex.LOW);
            preventive.setWorkDescription("Rutina preventiva ilustrativa basada en la ficha técnica; validar la ejecución en campo.");
            preventive.setTechnicianNotes("Verificar lubricación, fijaciones, elementos de seguridad y condición general.");
            preventive.setNextMaintenanceDate(executedAt.toLocalDate().plusDays(
                    asset.getCategory().getMaintenanceFrequencyDays()));
            preventive.setHorometerAtExecution(asset.getCurrentHorometer());
            addTask(preventive, "Inspección visual y limpieza", true);
            addTask(preventive, "Revisión de lubricación y puntos de ajuste", true);
            addTask(preventive, "Prueba funcional y registro de novedades", true);
            preventive = preventiveMaintenanceRepository.save(preventive);

            MaintenanceHistory history = new MaintenanceHistory();
            history.setAsset(asset);
            history.setPreventiveMaintenance(preventive);
            history.setMaintenanceDate(executedAt.toLocalDate());
            history.setType(MaintenanceType.PREVENTIVE);
            history.setStatus(MaintenanceHistoryStatus.COMPLETED);
            history.setScheduledAt(executedAt);
            history.setExecutedAt(executedAt.plusHours(2));
            history.setTechnicianName(technician.getNombre());
            history.setTasksDescription("Registro de demostración: inspección preventiva ilustrativa; validar antes de tratarlo como intervención real.");
            history.setCost(BigDecimal.valueOf(85000L + index * 12000L));
            history.setNextRecommendedDate(preventive.getNextMaintenanceDate());
            history.setHorometerAtMaintenance(asset.getCurrentHorometer());
            history.setTestsPerformed("Lista de comprobación preventiva y prueba funcional ilustrativas.");
            history.setFinalEquipmentState(finalStateFor(asset));
            history.setRecommendations("Confirmar la inspección y actualizar el horómetro durante el mantenimiento real.");
            maintenanceHistoryRepository.save(history);
        }

        for (int index = 0; index < 5; index++) {
            Asset asset = assets.get(index);
            PreventiveMaintenance scheduled = new PreventiveMaintenance();
            scheduled.setAsset(asset);
            scheduled.setTechnician(technicians.get(index));
            scheduled.setScheduledAt(now.plusDays(2L + index * 2L));
            scheduled.setStatus(PreventiveMaintenanceStatus.SCHEDULED);
            scheduled.setAttentionIndex(MaintenanceAttentionIndex.MEDIUM);
            scheduled.setPredictiveContext("Programación de demostración: revisar recomendaciones del diagnóstico antes de asignar la orden.");
            addTask(scheduled, "Confirmar disponibilidad del equipo", false);
            addTask(scheduled, "Realizar inspección de seguridad", false);
            addTask(scheduled, "Registrar horómetro y evidencias", false);
            preventiveMaintenanceRepository.save(scheduled);
        }
    }

    private void addTask(PreventiveMaintenance maintenance, String description, boolean completed) {
        PreventiveMaintenanceTask task = new PreventiveMaintenanceTask();
        task.setMaintenance(maintenance);
        task.setDescription(description);
        task.setCompleted(completed);
        maintenance.getTasks().add(task);
    }

    private void createFailureReports(
            List<Asset> assets,
            List<Usuario> operators,
            List<Usuario> technicians,
            List<Usuario> supervisors,
            List<WorkGroup> groups) {
        String[] symptoms = {
                "Vibración inusual durante el trabajo", "Pérdida de potencia bajo carga",
                "Fuga visible en conexión hidráulica", "Desgaste que requiere revisión de seguridad",
                "Dificultad de arranque reportada por el operador", "Holgura en punto de acople",
                "Ruido intermitente en conjunto de transmisión", "Elemento de corte requiere inspección"
        };
        FailureStatus[] statuses = {
                FailureStatus.RESUELTO, FailureStatus.EN_EJECUCION, FailureStatus.ESPERA_REPUESTO,
                FailureStatus.ASIGNADA, FailureStatus.RESUELTO, FailureStatus.EN_EJECUCION,
                FailureStatus.ASIGNADA, FailureStatus.RESUELTO
        };
        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < symptoms.length; index++) {
            Asset asset = assets.get(index);
            Usuario technician = technicians.get(index % technicians.size());
            Usuario operator = operators.get(index);
            FailureReport report = new FailureReport();
            report.setAsset(asset);
            report.setUser(operator);
            report.setGroup(groups.get(index % groups.size()));
            report.setAssignedTechnician(technician);
            report.setTipoFalla("Inspección mecánica");
            report.setPrioridad(index % 4 == 0 ? FailurePriority.ALTA : FailurePriority.MEDIA);
            report.setDescripcion("Registro de demostración: " + symptoms[index]
                    + ". Confirmar diagnóstico y estado en una inspección real.");
            report.setFechaReporte(now.minusDays(index + 1L));
            report.setFechaAsignacion(report.getFechaReporte().plusHours(1));
            report.setEstado(statuses[index]);
            if (statuses[index] == FailureStatus.EN_EJECUCION
                    || statuses[index] == FailureStatus.ESPERA_REPUESTO
                    || statuses[index] == FailureStatus.RESUELTO) {
                report.setFechaInicioRevision(report.getFechaReporte().plusHours(2));
            }
            if (statuses[index] == FailureStatus.RESUELTO) {
                report.setFechaCierre(report.getFechaReporte().plusDays(1));
                report.setFechaEnvioCierre(report.getFechaCierre().minusHours(1));
            }
            report = failureReportRepository.save(report);

            if (statuses[index] == FailureStatus.RESUELTO) {
                createCorrectiveHistory(asset, technician, supervisors.get(index % supervisors.size()), report, now, index);
            }
        }
    }

    private void createCorrectiveHistory(
            Asset asset, Usuario technician, Usuario reviewer, FailureReport report, LocalDateTime now, int index) {
        MaintenanceHistory history = new MaintenanceHistory();
        history.setAsset(asset);
        history.setFailureReport(report);
        history.setMaintenanceDate(report.getFechaCierre().toLocalDate());
        history.setType(MaintenanceType.CORRECTIVE);
        history.setStatus(MaintenanceHistoryStatus.COMPLETED);
        history.setScheduledAt(report.getFechaReporte());
        history.setExecutedAt(report.getFechaCierre());
        history.setTechnicianName(technician.getNombre());
        history.setTasksDescription("Registro de demostración: intervención correctiva ilustrativa; validar antes de usar como historial real.");
        history.setInitialReportedSymptom(report.getDescripcion());
        history.setActualFailure("Causa por confirmar durante una intervención real.");
        history.setRootCause("Dato ilustrativo pendiente de validación técnica.");
        history.setWorkMinutes(90 + index * 15);
        history.setHorometerAtMaintenance(asset.getCurrentHorometer());
        history.setTestsPerformed("Prueba funcional ilustrativa; confirmar el resultado con el técnico responsable.");
        history.setFinalEquipmentState(finalStateFor(asset));
        history.setRecommendations("Documentar repuestos, mediciones y resultado verificado en campo.");
        history.setReviewedBy(reviewer.getNombre());
        history.setReviewedAt(now.minusDays(index));
        history.setReviewObservations("Revisión ilustrativa; requiere validación operativa.");
        history.setCost(BigDecimal.valueOf(125000L + index * 28000L));
        maintenanceHistoryRepository.save(history);
    }

    private EquipmentFinalState finalStateFor(Asset asset) {
        return switch (asset.getOperationalStatus()) {
            case OPERATIONAL_WITH_RESTRICTIONS -> EquipmentFinalState.OPERATIONAL_WITH_RESTRICTIONS;
            case IN_REPAIR, OUT_OF_SERVICE, OBSOLETE -> EquipmentFinalState.INOPERATIVE_WAITING_PART;
            default -> EquipmentFinalState.OPERATIONAL;
        };
    }

    private void createPredictiveRecords(List<Asset> assets) {
        List<PredictiveRecord> records = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int assetIndex = 0; assetIndex < assets.size(); assetIndex++) {
            Asset asset = assets.get(assetIndex);
            if (!asset.isHasMotor()) {
                continue;
            }
            for (int readingIndex = 0; readingIndex < 6; readingIndex++) {
                PredictiveRecord record = new PredictiveRecord();
                record.setAsset(asset);
                record.setMachineId("MOTOR-" + asset.getId());
                record.setTimestamp(now.minusHours((long) (6 - readingIndex) * 12));
                record.setTemperature(68 + assetIndex % 8 + readingIndex * 1.4);
                record.setVibration(1.2 + (assetIndex % 4) * 0.2 + readingIndex * 0.08);
                record.setHumidity(48 + (assetIndex * 3 + readingIndex) % 18);
                record.setPressure(98 + (assetIndex % 5) * 1.2);
                record.setEnergyConsumption(3.8 + (assetIndex % 7) * 0.4 + readingIndex * 0.1);
                record.setMachineStatus(1);
                record.setPredictionClass(readingIndex == 5 && assetIndex % 3 == 0 ? 1 : 0);
                record.setTipoFallaPredicha(record.getPredictionClass() == 1 ? "Vibración elevada" : "Sin alerta");
                record.setGuardarParaReentrenamiento(false);
                record.setMotorState("OPERATIVO");
                record.setConfidence(record.getPredictionClass() == 1 ? 0.78 : 0.91);
                record.setAffectedComponents(record.getPredictionClass() == 1 ? "Rodamientos; transmisión" : "Sin componente identificado");
                record.setTendenciaLstm(record.getPredictionClass() == 1 ? "TENDENCIA_AL_ALZA" : "ESTABLE");
                record.setConfianzaLstm(record.getPredictionClass() == 1 ? 0.72 : 0.89);
                record.setRazonConfianza("Serie ilustrativa para visualizar la tendencia; no corresponde a una lectura de sensor.");
                records.add(record);
            }
        }
        predictiveRecordRepository.saveAll(records);
    }

    private void createInventory(List<WorkGroup> groups, List<Usuario> storekeepers) {
        String[][] items = {
                {"Filtro de aceite para motor diésel", "Filtros", "und", "12", "4"},
                {"Filtro de combustible", "Filtros", "und", "10", "3"},
                {"Aceite hidráulico ISO 68", "Lubricantes", "L", "40", "12"},
                {"Grasa multipropósito EP2", "Lubricantes", "kg", "18", "5"},
                {"Correa de transmisión agrícola", "Transmisión", "und", "6", "2"},
                {"Cuchilla para cortamaleza", "Corte", "und", "16", "5"},
                {"Bujía para motor pequeño", "Encendido", "und", "12", "4"},
                {"Manguera hidráulica reforzada", "Hidráulica", "m", "20", "6"},
                {"Rodamiento agrícola", "Rodamientos", "und", "8", "2"},
                {"Guante de protección mecánica", "Seguridad", "par", "30", "10"}
        };
        for (int index = 0; index < items.length; index++) {
            InventoryItem item = new InventoryItem();
            item.setName(items[index][0]);
            item.setCategory(items[index][1]);
            item.setUnit(items[index][2]);
            item.setStock(new BigDecimal(items[index][3]));
            item.setMinimum(new BigDecimal(items[index][4]));
            item.setWorkGroup(groups.get(index % groups.size()));
            item = inventoryItemRepository.save(item);

            InventoryMovement movement = new InventoryMovement();
            movement.setItem(item);
            movement.setTipo(InventoryMovementType.ENTRADA);
            movement.setCantidad(item.getStock());
            Usuario storekeeper = storekeepers.get(index % storekeepers.size());
            movement.setResponsable(storekeeper.getNombre());
            movement.setRol(storekeeper.getRol().toLowerCase(Locale.ROOT));
            movement.setMotivo("Saldo inicial ilustrativo de bodega; verificar existencias físicas.");
            inventoryMovementRepository.save(movement);
        }
    }

    private record AssetDefinition(
            String name,
            String brand,
            String model,
            String serialNumber,
            String inventoryCode,
            String category,
            boolean hasMotor,
            OperationalStatus status,
            String reportNotes) {
    }
}
