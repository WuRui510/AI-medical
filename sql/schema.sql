-- ============================================================
--  AI 医疗助手 - 数据库初始化脚本
--  数据库：MySQL 8.0+
--  用法：mysql -u root -p < sql/schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `medical`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `medical`;

-- ------------------------------------------------------------
--  预约挂号表
--  对应实体：org.example.bean.Appointment
--  说明：日期与时间使用字符串存储，与 LangChain4j 工具调用的
--        入参格式保持一致（日期如 2025-04-14，时间如 上午/下午）。
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `appointment`;

CREATE TABLE `appointment`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    `username`    VARCHAR(50)  NOT NULL COMMENT '就诊人姓名',
    `id_card`     VARCHAR(18)  NOT NULL COMMENT '就诊人身份证号',
    `department`  VARCHAR(50)  NOT NULL COMMENT '预约科室',
    `date`        VARCHAR(20)  NOT NULL COMMENT '预约日期，格式 2025-04-14',
    `time`        VARCHAR(10)  NOT NULL COMMENT '预约时段：上午 / 下午',
    `doctor_name` VARCHAR(50)  DEFAULT NULL COMMENT '预约医生，可为空',
    PRIMARY KEY (`id`),
    KEY `idx_appointment_uk` (`username`, `id_card`, `department`, `date`, `time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='预约挂号记录';

-- ------------------------------------------------------------
--  MongoDB 说明（无需在此建表）
--  会话记忆由 MongoChatMemoryStore 持久化到：
--      数据库 : chat_memory_db
--      集合   : chat_message   （由 @Document("chat_message") 指定）
--      文档结构: { memoryId, content }，content 为序列化后的消息 JSON
--
--  首次启动后自动创建，可通过以下命令确认：
--      use chat_memory_db
--      db.chat_message.find().pretty()
-- ------------------------------------------------------------
