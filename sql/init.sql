SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS tree_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id BIGINT DEFAULT NULL,
    level INT NOT NULL COMMENT '1=街区, 2=路段, 3=点位',
    name VARCHAR(100) NOT NULL,
    sort_order INT DEFAULT 0,
    capacity INT DEFAULT NULL COMMENT '可摆放长凳上限(仅level=3点位)',
    capacity_updated_at TIMESTAMP NULL DEFAULT NULL COMMENT '容量最近调整时间',
    capacity_updated_reason VARCHAR(500) DEFAULT NULL COMMENT '容量最近调整原因',
    closed TINYINT DEFAULT 0 COMMENT '封闭状态：1-封闭中，0-未封闭(仅level=3点位)',
    closed_start_at DATETIME NULL DEFAULT NULL COMMENT '封闭开始时间',
    closed_end_at DATETIME NULL DEFAULT NULL COMMENT '封闭结束时间(到期自动解封)',
    closed_reason VARCHAR(500) DEFAULT NULL COMMENT '封闭原因',
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
    retired TINYINT DEFAULT 0 COMMENT '退役状态：1-已退役(报废)，0-在用',
    retirement_id BIGINT DEFAULT NULL COMMENT '退役记录ID',
    retired_reason VARCHAR(500) COMMENT '报废退役原因',
    retired_at DATETIME NULL COMMENT '退役时间',
    retired_by VARCHAR(50) COMMENT '退役经办人',
    replaced_by_bench_id BIGINT DEFAULT NULL COMMENT '替换新凳ID(原点位登记的替换长凳)',
    replaces_bench_id BIGINT DEFAULT NULL COMMENT '被替换的退役长凳ID(仅替换新凳有值)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_node_id (node_id),
    INDEX idx_code (code),
    INDEX idx_retired (retired),
    CONSTRAINT fk_bench_node FOREIGN KEY (node_id) REFERENCES tree_node(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='长凳档案表';

CREATE TABLE IF NOT EXISTS bench_retirement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bench_id BIGINT NOT NULL COMMENT '退役长凳ID',
    bench_code VARCHAR(50) NOT NULL COMMENT '退役长凳编号(台账快照)',
    node_id BIGINT NOT NULL COMMENT '退役时所在点位ID(原点位)',
    retired_reason VARCHAR(500) NOT NULL COMMENT '报废退役原因',
    retired_at DATETIME NOT NULL COMMENT '退役时间',
    retired_by VARCHAR(50) NOT NULL COMMENT '退役经办人',
    replacement_bench_id BIGINT DEFAULT NULL COMMENT '原点位登记的替换新凳ID',
    replacement_bench_code VARCHAR(50) DEFAULT NULL COMMENT '替换新凳编号',
    replaced_at DATETIME NULL COMMENT '替换登记时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bench_id (bench_id),
    INDEX idx_node_id (node_id),
    INDEX idx_retired_at (retired_at),
    INDEX idx_replacement (replacement_bench_id),
    CONSTRAINT fk_retirement_bench FOREIGN KEY (bench_id) REFERENCES bench(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='长凳报废退役台账表';

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

CREATE TABLE IF NOT EXISTS node_capacity_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT NOT NULL COMMENT '点位ID(level=3)',
    old_capacity INT DEFAULT NULL COMMENT '调整前容量(NULL表示新建初始化)',
    new_capacity INT NOT NULL COMMENT '调整后容量',
    occupied_count INT DEFAULT 0 COMMENT '调整时占用数',
    adjust_reason VARCHAR(500) COMMENT '调整原因',
    adjusted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '调整时间',
    adjusted_by VARCHAR(50) DEFAULT 'system',
    INDEX idx_node_id (node_id),
    INDEX idx_adjusted_at (adjusted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点位容量调整记录表';

CREATE TABLE IF NOT EXISTS node_closure_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id BIGINT NOT NULL COMMENT '点位ID(level=3)',
    action_type TINYINT NOT NULL COMMENT '动作类型：1-封闭，2-人工解封，3-到期自动解封',
    closed_start_at DATETIME NULL COMMENT '封闭开始时间',
    closed_end_at DATETIME NULL COMMENT '封闭结束时间',
    closed_reason VARCHAR(500) COMMENT '封闭原因',
    reopen_reason VARCHAR(500) COMMENT '解封原因(人工解封时填写)',
    operated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    operated_by VARCHAR(50) DEFAULT 'system',
    INDEX idx_node_id (node_id),
    INDEX idx_operated_at (operated_at),
    CONSTRAINT fk_closure_node FOREIGN KEY (node_id) REFERENCES tree_node(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点位封闭/解封记录表';

CREATE TABLE IF NOT EXISTS bench_inspection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bench_id BIGINT NOT NULL COMMENT '被巡检长凳ID',
    scope_node_id BIGINT COMMENT '巡检发起范围节点ID(街区/路段/点位)',
    inspected_at DATETIME NOT NULL COMMENT '检查时间',
    result TINYINT NOT NULL COMMENT '巡检结果：1-正常，0-异常',
    problem_type VARCHAR(50) COMMENT '问题类型(如结构松动/漆面破损/椅面开裂等)',
    severity TINYINT COMMENT '严重程度：1-低，2-中，3-高(仅异常时填写)',
    description VARCHAR(1000) COMMENT '问题描述',
    suggestion VARCHAR(1000) COMMENT '处理建议',
    inspector VARCHAR(50) DEFAULT 'system' COMMENT '检查人',
    repair_order_id BIGINT COMMENT '由该巡检异常生成的维修工单ID',
    task_id BIGINT COMMENT '来源巡检任务ID(由计划任务执行产生)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bench_id (bench_id),
    INDEX idx_inspected_at (inspected_at),
    INDEX idx_result_severity (result, severity),
    INDEX idx_task_id (task_id),
    CONSTRAINT fk_inspection_bench FOREIGN KEY (bench_id) REFERENCES bench(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='长凳巡检记录表';

CREATE TABLE IF NOT EXISTS inspection_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '计划名称',
    scope_node_id BIGINT NOT NULL COMMENT '巡检范围节点ID(街区/路段/点位)',
    cycle_type TINYINT NOT NULL COMMENT '巡检周期：1-每天，2-每周，3-每月',
    plan_date DATE NOT NULL COMMENT '首次计划日期',
    inspector VARCHAR(50) NOT NULL COMMENT '检查人',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '启用状态：1-启用，0-停用',
    remark VARCHAR(500) COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_scope_node_id (scope_node_id),
    INDEX idx_enabled (enabled),
    CONSTRAINT fk_plan_node FOREIGN KEY (scope_node_id) REFERENCES tree_node(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡检计划表';

CREATE TABLE IF NOT EXISTS inspection_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL COMMENT '所属巡检计划ID',
    scope_node_id BIGINT NOT NULL COMMENT '巡检范围节点快照',
    plan_date DATE NOT NULL COMMENT '计划执行日期',
    inspector VARCHAR(50) COMMENT '检查人',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-待执行，2-已执行(逾期由计划日期推导)',
    executed_at DATETIME NULL COMMENT '执行时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_plan_date (plan_id, plan_date),
    INDEX idx_plan_date (plan_date),
    INDEX idx_status (status),
    CONSTRAINT fk_task_plan FOREIGN KEY (plan_id) REFERENCES inspection_plan(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='巡检任务表';

CREATE TABLE IF NOT EXISTS repair_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT '维修工单号',
    bench_id BIGINT NOT NULL COMMENT '维修长凳ID',
    inspection_id BIGINT COMMENT '来源巡检记录ID',
    problem_type VARCHAR(50) COMMENT '问题类型',
    severity TINYINT COMMENT '严重程度：1-低，2-中，3-高',
    description VARCHAR(1000) COMMENT '问题描述',
    suggestion VARCHAR(1000) COMMENT '处理建议',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-待处理，2-维修中，3-已完成，4-已关闭',
    repair_result VARCHAR(1000) COMMENT '维修结果(完成时必填)',
    started_at DATETIME NULL COMMENT '开始维修时间',
    completed_at DATETIME NULL COMMENT '维修完成时间(完成时必填)',
    closed_at DATETIME NULL COMMENT '工单关闭时间',
    created_by VARCHAR(50) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bench_id (bench_id),
    INDEX idx_status (status),
    INDEX idx_severity (severity),
    CONSTRAINT fk_repair_bench FOREIGN KEY (bench_id) REFERENCES bench(id),
    CONSTRAINT fk_repair_inspection FOREIGN KEY (inspection_id) REFERENCES bench_inspection(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='长凳维修工单表';

CREATE TABLE IF NOT EXISTS point_lighting_inspection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    point_id BIGINT NOT NULL COMMENT '点位ID(level=3)',
    inspected_at DATETIME NOT NULL COMMENT '巡查时间',
    result TINYINT NOT NULL COMMENT '照明结论：1-完好，0-异常',
    lamp_count INT NOT NULL DEFAULT 0 COMMENT '灯具数量',
    problem_type VARCHAR(20) COMMENT '异常类型：缺灯/损坏(仅异常时填写)',
    description VARCHAR(1000) COMMENT '问题描述',
    inspector VARCHAR(50) NOT NULL COMMENT '巡查人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_point_id (point_id),
    INDEX idx_inspected_at (inspected_at),
    CONSTRAINT fk_lighting_point FOREIGN KEY (point_id) REFERENCES tree_node(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点位夜间照明巡查记录表';

CREATE TABLE IF NOT EXISTS point_sunshade_inspection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    point_id BIGINT NOT NULL COMMENT '点位ID(level=3)',
    inspected_at DATETIME NOT NULL COMMENT '巡查时间',
    result TINYINT NOT NULL COMMENT '遮阳棚结论：1-完好，0-异常(破损)',
    damaged_area DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '破损面积(平方米)，完好为0',
    damaged_location VARCHAR(200) COMMENT '破损位置(异常时必填)',
    description VARCHAR(1000) COMMENT '备注说明',
    inspector VARCHAR(50) NOT NULL COMMENT '巡查人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_point_id (point_id),
    INDEX idx_inspected_at (inspected_at),
    CONSTRAINT fk_sunshade_point FOREIGN KEY (point_id) REFERENCES tree_node(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点位遮阳棚巡查记录表';

CREATE TABLE IF NOT EXISTS additional_bench_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    district_id BIGINT NOT NULL COMMENT '所属街区ID(level=1)',
    section_id BIGINT NOT NULL COMMENT '投放路段ID(level=2)',
    plan_date DATE NOT NULL COMMENT '计划投放日期',
    bench_count INT NOT NULL COMMENT '计划加凳数量',
    verified_count INT NOT NULL DEFAULT 0 COMMENT '已核销投放数量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-待投放，2-已投放(逾期按计划日期动态推导)',
    remark VARCHAR(500) COMMENT '备注',
    last_verified_by VARCHAR(50) COMMENT '最近核销操作人',
    last_verified_at DATETIME NULL COMMENT '最近核销时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_section_plan_date (section_id, plan_date),
    INDEX idx_status (status),
    CONSTRAINT fk_add_plan_district FOREIGN KEY (district_id) REFERENCES tree_node(id),
    CONSTRAINT fk_add_plan_section FOREIGN KEY (section_id) REFERENCES tree_node(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节假日临时加凳预案表';

CREATE TABLE IF NOT EXISTS additional_bench_plan_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL COMMENT '加凳预案ID',
    verified_count INT NOT NULL COMMENT '本次核销数量',
    before_count INT NOT NULL COMMENT '核销前累计数量',
    after_count INT NOT NULL COMMENT '核销后累计数量',
    operator VARCHAR(50) NOT NULL COMMENT '核销操作人',
    remark VARCHAR(500) COMMENT '核销备注',
    verified_at DATETIME NOT NULL COMMENT '核销时间',
    INDEX idx_plan_id (plan_id),
    INDEX idx_verified_at (verified_at),
    CONSTRAINT fk_add_plan_log_plan FOREIGN KEY (plan_id) REFERENCES additional_bench_plan(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加凳投放核销记录表';

CREATE TABLE IF NOT EXISTS bench_sponsorship (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    point_id BIGINT NOT NULL COMMENT '冠名点位ID(level=3)',
    merchant_name VARCHAR(100) NOT NULL COMMENT '冠名商户名称',
    sponsorship_text VARCHAR(200) NOT NULL COMMENT '冠名文案',
    start_date DATE NOT NULL COMMENT '冠名开始日期',
    end_date DATE NOT NULL COMMENT '冠名结束日期(过期按结束日期动态推导)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_point_date (point_id, start_date, end_date),
    CONSTRAINT fk_sponsorship_point FOREIGN KEY (point_id) REFERENCES tree_node(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户长凳冠名台账表';

INSERT INTO tree_node (parent_id, level, name, sort_order, capacity) VALUES
(NULL, 1, '商业步行街A区', 1, NULL),
(NULL, 1, '商业步行街B区', 2, NULL),
(1, 2, 'A区主干道', 1, NULL),
(1, 2, 'A区支路一', 2, NULL),
(1, 2, 'A区支路二', 3, NULL),
(2, 2, 'B区主干道', 1, NULL),
(2, 2, 'B区支路', 2, NULL),
(3, 3, 'A区主干道-广场前', 1, 2),
(3, 3, 'A区主干道-商店旁', 2, 10),
(3, 3, 'A区主干道-路口处', 3, 5),
(4, 3, 'A区支路一-公园边', 1, 3),
(4, 3, 'A区支路一-居民区', 2, 5),
(5, 3, 'A区支路二-学校旁', 1, 8),
(6, 3, 'B区主干道-地铁站口', 1, 10),
(6, 3, 'B区主干道-商场前', 2, 6),
(7, 3, 'B区支路-小区门口', 1, 10);

INSERT INTO node_capacity_log (node_id, old_capacity, new_capacity, occupied_count, adjust_reason, adjusted_by) VALUES
(8, NULL, 2, 0, '新建点位，初始化容量', 'system'),
(9, NULL, 10, 0, '新建点位，初始化容量', 'system'),
(10, NULL, 5, 0, '新建点位，初始化容量', 'system'),
(11, NULL, 3, 0, '新建点位，初始化容量', 'system'),
(12, NULL, 5, 0, '新建点位，初始化容量', 'system'),
(13, NULL, 8, 0, '新建点位，初始化容量', 'system'),
(14, NULL, 10, 0, '新建点位，初始化容量', 'system'),
(15, NULL, 6, 0, '新建点位，初始化容量', 'system'),
(16, NULL, 10, 0, '新建点位，初始化容量', 'system');

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
