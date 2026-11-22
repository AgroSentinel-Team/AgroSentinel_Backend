package com.agrosentinel.agrosentinel.services;

import java.util.List;

import com.agrosentinel.agrosentinel.models.SpecDataType;

/**
 * Plantillas sugeridas de especificaciones por familia de equipo agrícola. Se armaron con los
 * campos que aparecen en las fichas técnicas reales de maquinaria; el administrador puede
 * aplicarlas a una categoría y luego ajustarlas.
 */
public final class SpecPresets {
    public record Field(
            String key,
            String label,
            SpecDataType type,
            String unit,
            List<String> options,
            boolean required,
            String helpText) {
    }

    public record Preset(String key, String name, String description, List<Field> fields) {
    }

    private static final List<String> HITCH = List.of("Tres puntos", "Barra de tiro", "Toma de fuerza (TDF)");
    private static final List<String> DRIVE = List.of("Cardán (TDF)", "Cilindro hidráulico", "Arrastre", "Correas");
    private static final List<String> FUEL = List.of("Diésel", "Gasolina", "Mezcla (2T)");

    private static final List<Preset> PRESETS = List.of(
            new Preset("TRACTOR", "Tractores",
                    "Tractores agrícolas y equipos autopropulsados con motor diésel.",
                    List.of(
                            text("engine", "Motor", true, "Marca y modelo, p. ej. Perkins A4248"),
                            text("engine_number", "Número de motor", false, null),
                            number("power_hp", "Potencia", "HP", true),
                            number("rated_rpm", "Régimen nominal", "rpm", false),
                            number("cylinders", "Cilindros", null, false),
                            option("fuel", "Combustible", FUEL, false),
                            text("transmission", "Transmisión", false, "Sencilla, doble, sincronizada..."),
                            number("electrical_system", "Sistema eléctrico", "V", false),
                            text("hydraulic_system", "Sistema hidráulico", false, "Centro abierto, modelo..."),
                            text("front_tires", "Llantas delanteras", false, "Medida, p. ej. 7.50-16"),
                            text("rear_tires", "Llantas traseras", false, "Medida, p. ej. 15x30"))),
            new Preset("SMALL_ENGINE", "Equipos de motor pequeño",
                    "Guadañas, motosierras, ahoyadoras, podadoras y fumigadoras motorizadas.",
                    List.of(
                            text("engine", "Motor", false, "Marca o referencia del motor"),
                            number("displacement_cc", "Cilindrada", "cc", true),
                            number("power_hp", "Potencia", "HP", false),
                            option("engine_cycle", "Tipo de motor", List.of("2 tiempos", "4 tiempos"), false),
                            option("fuel", "Combustible", FUEL, false),
                            number("tank_l", "Tanque", "L", false),
                            option("shaft", "Eje", List.of("Rígido", "Flexible"), false),
                            text("working_tool", "Herramienta de trabajo", false,
                                    "Cuchilla, cadena, barreno (largo x diámetro)..."))),
            new Preset("TILLAGE", "Implementos de labranza y corte",
                    "Rastras, rastrillos, arados, cultivadoras, desbrozadoras, cortamalezas y palas.",
                    List.of(
                            option("hitch", "Acople", HITCH, true),
                            option("drive", "Accionamiento", DRIVE, false),
                            number("working_width_m", "Ancho de trabajo", "m", false),
                            number("disc_count", "Número de discos", null, false),
                            number("disc_diameter_in", "Diámetro de discos", "pulg", false),
                            number("shanks", "Vástagos o brazos", null, false),
                            number("rows", "Surcos", null, false),
                            text("tires", "Llantas", false, "Medida y cantidad"))),
            new Preset("SEEDING", "Siembra y fertilización",
                    "Sembradoras, plantadoras, abonadoras y transplantadoras.",
                    List.of(
                            number("rows", "Filas o surcos", null, true),
                            text("crop_use", "Cultivo o uso", false, "Grano fino, grano grueso, arroz, fertilizante..."),
                            option("metering", "Dosificación", List.of("Mecánica", "Neumática", "Al voleo"), false),
                            text("hoppers", "Tolvas", false, "Semilla, fertilizante..."),
                            option("hitch", "Acople", HITCH, false),
                            option("drive", "Accionamiento", DRIVE, false),
                            number("power_hp", "Potencia (si es autopropulsada)", "HP", false))),
            new Preset("TRANSPORT", "Transporte",
                    "Remolques, planchones y carros de carga.",
                    List.of(
                            number("load_capacity_kg", "Capacidad de carga", "kg", true),
                            text("platform_dimensions", "Dimensiones de plataforma", false, "Largo x ancho, p. ej. 4 x 2 m"),
                            text("platform_material", "Plataforma", false, "Metálica, madera..."),
                            number("axles", "Ejes", null, false),
                            text("tires", "Llantas", false, "Medida y lonas, p. ej. 750x16, 10 lonas"),
                            text("feature", "Característica", false, null))),
            new Preset("SPRAYING", "Fumigación y aplicación",
                    "Aguilones, fumigadoras de espalda, de lanza, de polvo y estacionarias.",
                    List.of(
                            option("sprayer_type", "Tipo de aplicación",
                                    List.of("De espalda", "Lanza", "Polvo", "Estacionaria", "Aguilón"), true),
                            number("tank_l", "Tanque", "L", false),
                            number("boom_length_m", "Longitud del aguilón", "m", false),
                            number("nozzles", "Boquillas", null, false),
                            text("engine", "Motor", false, null),
                            number("displacement_cc", "Cilindrada", "cc", false),
                            text("hoses", "Mangueras", false, null))),
            new Preset("PUMPING", "Bombeo, riego y compresores",
                    "Motobombas, carretes de riego y compresores.",
                    List.of(
                            option("power_source", "Fuente de potencia",
                                    List.of("Eléctrico", "Diésel", "Gasolina", "Toma de fuerza (TDF)"), true),
                            number("power_hp", "Potencia", "HP", false),
                            number("rated_rpm", "Velocidad", "rpm", false),
                            number("port_diameter_in", "Diámetro de succión/descarga", "pulg", false),
                            text("pressure", "Presión de trabajo", false, "p. ej. alta presión, 60-80 psi"),
                            text("starting", "Arranque", false, "Directo, manual, eléctrico..."),
                            text("tank", "Tanque", false, "Capacidad, válvula de drenaje..."))));

    private SpecPresets() {
    }

    public static List<Preset> all() {
        return PRESETS;
    }

    private static Field text(String key, String label, boolean required, String help) {
        return new Field(key, label, SpecDataType.TEXT, null, List.of(), required, help);
    }

    private static Field number(String key, String label, String unit, boolean required) {
        return new Field(key, label, SpecDataType.NUMBER, unit, List.of(), required, null);
    }

    private static Field option(String key, String label, List<String> options, boolean required) {
        return new Field(key, label, SpecDataType.OPTION, null, options, required, null);
    }
}
