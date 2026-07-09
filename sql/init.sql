SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS tree_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT NULL,
    level INT NOT NULL COMMENT '1=街区, 2=路段, 3=点位',
    name VARCHAR(100) NOT NULL,
    sort_order INT DEFAULT 0,
    is_deleted TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent_id (parent_id),
    INDEX idx_level (level),
    INDEX idx_is_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='树形节点表';

CREATE TABLE IF NOT EXISTS bench (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '长凳编号',
    material VARCHAR(50) COMMENT '材质',
    length DECIMAL(10,2) COMMENT '长度(cm)',
    width DECIMAL(10,2) COMMENT '宽度(cm)',
    height DECIMAL(10,2) COMMENT '高度(cm)',
    node_id BIGINT NOT NULL COMMENT '所属点位ID(level=3)',
    specs_json TEXT COMMENT '规格参数JSON',
    status TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-停用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_node_id (node_id),
    INDEX idx_code (code),
    CONSTRAINT fk_bench_node FOREIGN KEY (node_id) REFERENCES tree_node(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='长凳档案表';

CREATE TABLE IF NOT EXISTS bench_change_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bench_id BIGINT NOT NULL,
    old_node_id BIGINT NOT NULL,
    new_node_id BIGINT NOT NULL,
    change_reason VARCHAR(500) COMMENT '变更原因',
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(50) DEFAULT 'system',
    INDEX idx_bench_id (bench_id),
    INDEX idx_changed_at (changed_at),
    CONSTRAINT fk_log_bench FOREIGN KEY (bench_id) REFERENCES bench(id),
    CONSTRAINT fk_log_old_node FOREIGN KEY (old_node_id) REFERENCES tree_node(id),
    CONSTRAINT fk_log_new_node FOREIGN KEY (new_node_id) REFERENCES tree_node(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='长凳分类变更日志表';

INSERT INTO tree_node (parent_id, level, name, sort_order) VALUES
(NULL, 1, '商业步行街A区', 1),
(NULL, 1, '商业步行街B区', 2),
(1, 2, 'A区主干道', 1),
(1, 2, 'A区支路一', 2),
(1, 2, 'A区支路二', 3),
(2, 2, 'B区主干道', 1),
(2, 2, 'B区支路', 2),
(3, 3, 'A区主干道-广场前', 1),
(3, 3, 'A区主干道-商店旁', 2),
(3, 3, 'A区主干道-路口处', 3),
(4, 3, 'A区支路一-公园边', 1),
(4, 3, 'A区支路一-居民区', 2),
(5, 3, 'A区支路二-学校旁', 1),
(6, 3, 'B区主干道-地铁站口', 1),
(6, 3, 'B区主干道-商场前', 2),
(7, 3, 'B区支路-小区门口', 1);

INSERT INTO bench (code, material, length, width, height, node_id, specs_json, status) VALUES
('BNCH-A001', '实木', 150.00, 50.00, 45.00, 8, '{"seatCount":2,"weight":35,"capacity":200}', 1),
('BNCH-A002', '实木', 150.00, 50.00, 45.00, 8, '{"seatCount":2,"weight":35,"capacity":200}', 1),
('BNCH-A003', '铝合金', 180.00, 55.00, 48.00, 9, '{"seatCount":3,"weight":25,"capacity":300}', 1),
('BNCH-A004', '不锈钢', 200.00, 60.00, 50.00, 10, '{"seatCount":4,"weight":40,"capacity":400}', 1),
('BNCH-A005', '实木', 150.00, 50.00, 45.00, 11, '{"seatCount":2,"weight":35,"capacity":200}', 1),
('BNCH-A006', '铝合金', 180.00, 55.00, 48.00, 12, '{"seatCount":3,"weight":25,"capacity":300}', 1),
('BNCH-A007', '不锈钢', 200.00, 60.00, 50.00, 13, '{"seatCount":4,"weight":40,"capacity":400}', 1),
('BNCH-B001', '实木', 150.00, 50.00, 45.00, 14, '{"seatCount":2,"weight":35,"capacity":200}', 1),
('BNCH-B002', '铝合金', 180.00, 55.00, 48.00, 15, '{"seatCount":3,"weight":25,"capacity":300}', 1),
('BNCH-B003', '不锈钢', 200.00, 60.00, 50.00, 16, '{"seatCount":4,"weight":40,"capacity":400}', 1);
