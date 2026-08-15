-- SQL：结构化查询语言，用于管理和查询关系型数据库。
-- 本示例覆盖：DDL、DML、DQL、TCL、DCL、约束、索引、
--   事务、JOIN、子查询、窗口函数、CTE、视图、存储过程。

-- ── 1. DDL——数据定义语言 ──────────────────────────────
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100),
    age INT DEFAULT 18 CHECK (age >= 0 AND age <= 150),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    quantity INT DEFAULT 1 CHECK (quantity > 0),
    amount DECIMAL(10, 2) NOT NULL,
    status ENUM('pending', 'paid', 'shipped', 'cancelled') DEFAULT 'pending',
    order_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- 外键约束：关联 users 表
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE          -- 用户删除时级联删除订单
        ON UPDATE CASCADE          -- 用户ID更新时级联更新
);

CREATE TABLE payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(20),
    paid_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- 删除表
-- DROP TABLE IF EXISTS payments;
-- 修改表
-- ALTER TABLE users ADD COLUMN phone VARCHAR(20);
-- ALTER TABLE users DROP COLUMN phone;
-- ALTER TABLE users MODIFY COLUMN email VARCHAR(200);
-- 截断表（清空数据，重置自增）
-- TRUNCATE TABLE orders;

-- ── 2. DML——数据操作语言 ──────────────────────────────
-- INSERT：插入一行
INSERT INTO users (username, email, age)
VALUES ('Alice', 'alice@example.com', 25);

-- 插入多行
INSERT INTO users (username, email, age) VALUES
    ('Bob',   'bob@example.com',   30),
    ('Charlie', 'charlie@example.com', 35),
    ('Diana',   'diana@example.com',  28);

INSERT INTO orders (user_id, product_name, quantity, amount, status) VALUES
    (1, 'Product A',  2, 150.00, 'paid'),
    (1, 'Product B',  1,  89.90, 'pending'),
    (2, 'Product C',  3, 299.70, 'shipped'),
    (3, 'Product D',  1,  49.50, 'cancelled');

INSERT INTO payments (order_id, amount, payment_method) VALUES
    (1, 150.00, 'credit_card'),
    (3, 299.70, 'paypal');

-- UPDATE：修改数据
UPDATE users
SET email = 'new.alice@example.com', age = 26
WHERE username = 'Alice';

-- DELETE：删除数据
DELETE FROM orders
WHERE status = 'cancelled';

-- ── 3. DQL——数据查询语言 ─────────────────────────────
-- 基础 SELECT
SELECT * FROM users;

-- 选择特定列
SELECT username, email FROM users;

-- WHERE 过滤
SELECT * FROM users WHERE age > 25;

-- 比较运算符：=, <>, !=, <, <=, >, >=
SELECT * FROM users WHERE age BETWEEN 25 AND 35;

-- IN 运算符
SELECT * FROM users WHERE username IN ('Alice', 'Bob');

-- LIKE 模糊匹配
-- % 匹配任意字符序列，_ 匹配单个字符
SELECT * FROM users WHERE email LIKE '%@example.com';

-- IS NULL / IS NOT NULL
SELECT * FROM users WHERE email IS NOT NULL;

-- ORDER BY 排序：ASC 升序（默认），DESC 降序
SELECT * FROM users ORDER BY age DESC;

-- LIMIT 和 OFFSET
SELECT * FROM users ORDER BY user_id LIMIT 10 OFFSET 0;

-- DISTINCT 去重
SELECT DISTINCT status FROM orders;

-- ── 4. 聚合函数 ──────────────────────────────────────
SELECT
    COUNT(*)                AS total_users,         -- 行数
    COUNT(DISTINCT email)   AS unique_emails,        -- 去重计数
    AVG(age)                AS avg_age,              -- 平均值
    MIN(age)                AS min_age,              -- 最小值
    MAX(age)                AS max_age,              -- 最大值
    SUM(age)                AS sum_age               -- 求和
FROM users;

-- ── 5. GROUP BY 与 HAVING ───────────────────────────
-- HAVING 在分组后过滤，WHERE 在分组前过滤
SELECT
    status,
    COUNT(*)    AS order_count,
    SUM(amount) AS total_amount,
    AVG(amount) AS avg_amount
FROM orders
WHERE order_date >= '2024-01-01'     -- 分组前过滤
GROUP BY status
HAVING SUM(amount) > 100             -- 分组后过滤
ORDER BY total_amount DESC;

-- ── 6. JOIN——多表关联 ────────────────────────────────
-- INNER JOIN：两表匹配的行
SELECT u.username, o.product_name, o.amount, o.status
FROM users u
INNER JOIN orders o ON u.user_id = o.user_id
WHERE o.status = 'paid';

-- LEFT JOIN：保留左表所有行，右表无匹配则填 NULL
SELECT u.username, o.product_name, o.amount
FROM users u
LEFT JOIN orders o ON u.user_id = o.user_id;

-- RIGHT JOIN：保留右表所有行
SELECT u.username, o.product_name
FROM users u
RIGHT JOIN orders o ON u.user_id = o.user_id;

-- FULL OUTER JOIN（MySQL 不支持，用 UNION 模拟）
-- CROSS JOIN：笛卡尔积
SELECT u.username, o.product_name
FROM users u CROSS JOIN orders o;

-- SELF JOIN：自连接
-- 查找同岁的用户对
SELECT a.username AS user1, b.username AS user2, a.age
FROM users a
JOIN users b ON a.age = b.age AND a.user_id < b.user_id;

-- 多表 JOIN
SELECT
    u.username,
    o.product_name,
    o.amount AS order_amount,
    p.amount AS payment_amount,
    p.payment_method
FROM users u
JOIN orders o ON u.user_id = o.user_id
LEFT JOIN payments p ON o.order_id = p.order_id;

-- ── 7. 子查询 ────────────────────────────────────────
-- 标量子查询（返回单值）
SELECT username
FROM users
WHERE user_id = (SELECT MAX(user_id) FROM users);

-- 多行子查询（IN / NOT IN）
SELECT username
FROM users
WHERE user_id IN (
    SELECT user_id FROM orders WHERE amount > 100
);

-- EXISTS 子查询（比 IN 效率更高）
SELECT username
FROM users u
WHERE EXISTS (
    SELECT 1 FROM orders o
    WHERE o.user_id = u.user_id AND o.status = 'paid'
);

-- NOT EXISTS 查找未下过单的用户
SELECT username
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM orders o WHERE o.user_id = u.user_id
);

-- 相关子查询
SELECT username, (
    SELECT COUNT(*) FROM orders o WHERE o.user_id = u.user_id
) AS order_count
FROM users u;

-- ── 8. UNION ─────────────────────────────────────────
-- UNION 合并结果集（去重），UNION ALL 保留所有
SELECT username AS name, 'user' AS type FROM users
UNION ALL
SELECT product_name, 'product' FROM orders;

-- INTERSECT / EXCEPT（MySQL 不支持，可用 JOIN 模拟）

-- ── 9. CASE 表达式 ──────────────────────────────────
SELECT
    username,
    age,
    CASE
        WHEN age < 18 THEN '未成年'
        WHEN age BETWEEN 18 AND 60 THEN '成年'
        ELSE '老年'
    END AS age_group,
    -- 简单 CASE 形式
    CASE age
        WHEN 25 THEN '二十五岁'
        WHEN 30 THEN '三十岁'
        ELSE '其他'
    END AS age_label
FROM users;

-- ── 10. NULL 处理 ────────────────────────────────────
-- COALESCE：返回第一个非 NULL 值
SELECT username, COALESCE(email, '未提供邮箱') AS contact FROM users;

-- IFNULL（MySQL 特有）
-- NULLIF：若两个值相等则返回 NULL
SELECT NULLIF(age, 0) FROM users;

-- ── 11. 窗口函数 ─────────────────────────────────────
-- ROW_NUMBER：行号（每行唯一）
SELECT username, age,
    ROW_NUMBER() OVER (ORDER BY age DESC) AS rank_num
FROM users;

-- RANK：有重复值则跳过后续名次
SELECT username, age,
    RANK() OVER (PARTITION BY age ORDER BY username) AS rnk
FROM users;

-- DENSE_RANK：有重复值但不跳过名次
SELECT status, amount,
    DENSE_RANK() OVER (PARTITION BY status ORDER BY amount DESC) AS rnk
FROM orders;

-- 聚合窗口函数
SELECT username, order_date, amount,
    SUM(amount) OVER (PARTITION BY u.user_id) AS user_total_spent,
    AVG(amount) OVER () AS overall_avg
FROM users u
JOIN orders o ON u.user_id = o.user_id;

-- LAG / LEAD：前一行/后一行
SELECT order_id, amount,
    LAG(amount, 1) OVER (ORDER BY order_date) AS prev_amount,
    LEAD(amount, 1) OVER (ORDER BY order_date) AS next_amount
FROM orders;

-- ── 12. CTE——公用表表达式 ──────────────────────────
WITH paid_orders AS (
    SELECT * FROM orders WHERE status = 'paid'
),
user_spending AS (
    SELECT
        u.user_id,
        u.username,
        SUM(po.amount) AS total_spent
    FROM users u
    JOIN paid_orders po ON u.user_id = po.user_id
    GROUP BY u.user_id, u.username
)
SELECT * FROM user_spending
WHERE total_spent > 100
ORDER BY total_spent DESC;

-- 递归 CTE（生成 1 到 10）
-- WITH RECURSIVE seq(n) AS (
--     SELECT 1
--     UNION ALL
--     SELECT n + 1 FROM seq WHERE n < 10
-- )
-- SELECT * FROM seq;

-- ── 13. 索引 ─────────────────────────────────────────
-- 单列索引
CREATE INDEX idx_users_email ON users(email);
-- 复合索引
CREATE INDEX idx_orders_user_status ON orders(user_id, status);
-- 唯一索引
CREATE UNIQUE INDEX idx_users_username ON users(username);
-- 删除索引
-- DROP INDEX idx_users_email ON users;

-- ── 14. 事务 ─────────────────────────────────────────
-- START TRANSACTION 或 BEGIN
START TRANSACTION;

UPDATE users SET age = 26 WHERE username = 'Alice';
INSERT INTO orders (user_id, product_name, quantity, amount, status)
VALUES (1, 'Product E', 1, 59.90, 'paid');

-- SAVEPOINT 保存点
SAVEPOINT before_update;
UPDATE orders SET status = 'shipped' WHERE status = 'paid';
-- ROLLBACK TO SAVEPOINT before_update;  -- 回退到保存点

COMMIT;                                  -- 提交事务
-- ROLLBACK;                             -- 回滚事务

-- ── 15. 视图 ─────────────────────────────────────────
CREATE VIEW user_order_summary AS
SELECT
    u.user_id,
    u.username,
    COUNT(o.order_id) AS total_orders,
    COALESCE(SUM(o.amount), 0) AS total_spent
FROM users u
LEFT JOIN orders o ON u.user_id = o.user_id
GROUP BY u.user_id, u.username;

SELECT * FROM user_order_summary ORDER BY total_spent DESC;
-- DROP VIEW IF EXISTS user_order_summary;

-- ── 16. 存储过程 ─────────────────────────────────────
-- DELIMITER $$
-- CREATE PROCEDURE GetUserOrders(IN userId INT)
-- BEGIN
--     SELECT * FROM orders WHERE user_id = userId;
-- END$$
-- DELIMITER ;
-- CALL GetUserOrders(1);

-- ── 17. DCL——数据控制语言 ────────────────────────────
-- 授权
-- GRANT SELECT, INSERT ON users TO 'app_user'@'localhost';
-- 撤销权限
-- REVOKE DELETE ON orders FROM 'app_user'@'localhost';

-- ── 18. 常用内置函数 ─────────────────────────────────
-- 字符串：CONCAT(), UPPER(), LOWER(), SUBSTRING(), TRIM(), LENGTH()
-- 日期：NOW(), CURDATE(), DATE_ADD(), DATEDIFF(), YEAR()
-- 数学：ABS(), ROUND(), CEIL(), FLOOR(), RAND()
-- 类型转换：CAST(age AS CHAR), CONVERT(amount, CHAR)
