-- Script de creación de la base de datos para Gestión de Fincas e Inmuebles
CREATE DATABASE IF NOT EXISTS fincas_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE fincas_db;

-- 1. Tabla de Inquilinos
CREATE TABLE IF NOT EXISTS inquilinos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    dni VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    edad INT NOT NULL,
    sexo VARCHAR(10) NOT NULL,
    foto_path VARCHAR(255) NULL,
    metodo_garantia VARCHAR(50) NOT NULL, -- 'NOMINA', 'AVAL_BANCARIO', 'CONTRATO_TRABAJO', 'AVALADO_POR_OTRO'
    avalador_id INT NULL,
    FOREIGN KEY (avalador_id) REFERENCES inquilinos(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 2. Tabla de Inmuebles
-- Puede ser EDIFICIO, PISO o LOCAL. 
-- Si es un PISO o LOCAL dentro de un edificio, parent_edificio_id apunta al edificio.
CREATE TABLE IF NOT EXISTS inmuebles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL, -- 'EDIFICIO', 'PISO', 'LOCAL'
    direccion VARCHAR(150) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    codigo_postal VARCHAR(10) NOT NULL,
    planta VARCHAR(10) NULL, -- Solo para pisos/locales
    letra VARCHAR(10) NULL,   -- Solo para pisos/locales
    parent_edificio_id INT NULL,
    codigo_recibo VARCHAR(50) NULL UNIQUE, -- Código único de recibo constante en el tiempo
    FOREIGN KEY (parent_edificio_id) REFERENCES inmuebles(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. Tabla de Alquileres (Contratos activos e históricos)
CREATE TABLE IF NOT EXISTS alquileres (
    id INT AUTO_INCREMENT PRIMARY KEY,
    inmueble_id INT NOT NULL,
    inquilino_id INT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NULL, -- Nulo si el contrato sigue activo
    activo TINYINT(1) DEFAULT 1,
    FOREIGN KEY (inmueble_id) REFERENCES inmuebles(id) ON DELETE CASCADE,
    FOREIGN KEY (inquilino_id) REFERENCES inquilinos(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 4. Tabla de Bancos y Cuentas
CREATE TABLE IF NOT EXISTS bancos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre_banco VARCHAR(100) NOT NULL,
    numero_cuenta VARCHAR(50) NOT NULL UNIQUE,
    saldo DECIMAL(12, 2) NOT NULL DEFAULT 0.00
) ENGINE=InnoDB;

-- 5. Tabla de Recibos
-- Lleva un número de recibo único por piso/local constante en el tiempo (guardado como numero_recibo).
CREATE TABLE IF NOT EXISTS recibos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    inmueble_id INT NOT NULL,
    numero_recibo VARCHAR(50) NOT NULL, -- Copiado de inmuebles.codigo_recibo
    fecha_emision DATE NOT NULL,
    renta DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    agua DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    luz DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    ipc DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    porteria DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    iva DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    otros_conceptos DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    descripcion_otros VARCHAR(150) NULL,
    cobrado TINYINT(1) NOT NULL DEFAULT 0,
    FOREIGN KEY (inmueble_id) REFERENCES inmuebles(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. Tabla de Movimientos Bancarios
CREATE TABLE IF NOT EXISTS movimientos_bancarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    banco_id INT NOT NULL,
    tipo VARCHAR(10) NOT NULL, -- 'INGRESO' o 'GASTO'
    fecha DATE NOT NULL,
    importe DECIMAL(10, 2) NOT NULL,
    categoria VARCHAR(50) NOT NULL, -- 'REPARACION', 'LIMPIEZA', 'RECIBO_ALQUILER', 'SUELDO', 'OTROS_GASTOS', 'OTROS_INGRESOS'
    inmueble_id INT NULL, -- Asociado a un inmueble (requerido para gastos)
    piso_local_id INT NULL, -- Asociado a un piso/local (requerido para ingresos)
    FOREIGN KEY (banco_id) REFERENCES bancos(id) ON DELETE CASCADE,
    FOREIGN KEY (inmueble_id) REFERENCES inmuebles(id) ON DELETE SET NULL,
    FOREIGN KEY (piso_local_id) REFERENCES inmuebles(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- INSERT DE DATOS MOCK INICIALES PARA PRUEBAS
-- Bancos
INSERT INTO bancos (nombre_banco, numero_cuenta, saldo) VALUES 
('Banco Santander', 'ES12 3456 7890 1234 5678', 50000.00),
('BBVA', 'ES98 7654 3210 9876 5432', 12000.50);

-- Inquilinos
INSERT INTO inquilinos (dni, nombre, edad, sexo, foto_path, metodo_garantia, avalador_id) VALUES
('12345678A', 'Juan Pérez Gómez', 32, 'MASCULINO', '', 'NOMINA', NULL),
('87654321B', 'María López Fernández', 28, 'FEMENINO', '', 'CONTRATO_TRABAJO', NULL);

INSERT INTO inquilinos (dni, nombre, edad, sexo, foto_path, metodo_garantia, avalador_id) VALUES
('11223344C', 'Carlos García Ruiz', 21, 'MASCULINO', '', 'AVALADO_POR_OTRO', 1); -- Avalado por Juan Pérez

-- Inmuebles (Edificios, Pisos y Locales)
-- Edificio A
INSERT INTO inmuebles (tipo, direccion, numero, codigo_postal, planta, letra, parent_edificio_id, codigo_recibo) VALUES
('EDIFICIO', 'Avenida de la Constitución', '45', '28001', NULL, NULL, NULL, NULL);

-- Pisos y locales en Edificio A (id 1)
INSERT INTO inmuebles (tipo, direccion, numero, codigo_postal, planta, letra, parent_edificio_id, codigo_recibo) VALUES
('PISO', 'Avenida de la Constitución', '45', '28001', '1', 'A', 1, 'REC-ED1-P1A'),
('PISO', 'Avenida de la Constitución', '45', '28001', '1', 'B', 1, 'REC-ED1-P1B'),
('LOCAL', 'Avenida de la Constitución', '45', '28001', 'Bajo', 'Comercial 1', 1, 'REC-ED1-LOC1');

-- Inmuebles independientes (Piso y Local fuera de edificio gestionado completo)
INSERT INTO inmuebles (tipo, direccion, numero, codigo_postal, planta, letra, parent_edificio_id, codigo_recibo) VALUES
('PISO', 'Calle Mayor', '12', '28002', '3', 'Izquierda', NULL, 'REC-PISO-M12'),
('LOCAL', 'Plaza de España', '2', '28005', 'Bajo', 'A', NULL, 'REC-LOC-PE2');

-- Contratos de Alquiler activos
-- Piso 1A alquilado a Juan Pérez
INSERT INTO alquileres (inmueble_id, inquilino_id, fecha_inicio, fecha_fin, activo) VALUES
(2, 1, '2026-01-01', NULL, 1);

-- Piso 1B alquilado a María López
INSERT INTO alquileres (inmueble_id, inquilino_id, fecha_inicio, fecha_fin, activo) VALUES
(3, 2, '2026-02-15', NULL, 1);

-- Recibos históricos y del mes anterior (Abril)
INSERT INTO recibos (inmueble_id, numero_recibo, fecha_emision, renta, agua, luz, ipc, porteria, iva, otros_conceptos, descripcion_otros, cobrado) VALUES
(2, 'REC-ED1-P1A', '2026-04-01', 600.00, 25.00, 45.00, 12.00, 30.00, 126.00, 0.00, '', 1),
(3, 'REC-ED1-P1B', '2026-04-01', 650.00, 20.00, 50.00, 0.00, 30.00, 136.50, 0.00, '', 1);

-- Movimientos bancarios (para ingresos del cobro de recibos y gastos del edificio)
-- Ingresos de recibos cobrados en Abril
INSERT INTO movimientos_bancarios (banco_id, tipo, fecha, importe, categoria, inmueble_id, piso_local_id) VALUES
(1, 'INGRESO', '2026-04-05', 838.00, 'RECIBO_ALQUILER', NULL, 2),
(1, 'INGRESO', '2026-04-06', 886.50, 'RECIBO_ALQUILER', NULL, 3);

-- Gastos del edificio
INSERT INTO movimientos_bancarios (banco_id, tipo, fecha, importe, categoria, inmueble_id, piso_local_id) VALUES
(1, 'GASTO', '2026-04-10', 350.00, 'REPARACION', 1, NULL), -- Reparación ascensor
(1, 'GASTO', '2026-04-20', 120.00, 'LIMPIEZA', 1, NULL);   -- Limpieza portal
