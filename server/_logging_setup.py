# -*- coding: utf-8 -*-
"""统一文件日志落盘助手（勘误/运维用）。

所有 DraftPeek 辅助服务共用：控制台之外追加滚动文件日志，
写入 server/logs/<name>.log（`*.log` 已被 .gitignore 忽略，不入库）。
"""
import logging
import os
from logging.handlers import RotatingFileHandler

_LOGS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "logs")


def setup_file_logging(name: str, max_bytes: int = 5 * 1024 * 1024, backup_count: int = 3) -> None:
    """为 root logger 追加滚动文件 handler（幂等：同文件不重复添加）。"""
    os.makedirs(_LOGS_DIR, exist_ok=True)
    path = os.path.join(_LOGS_DIR, f"{name}.log")
    root = logging.getLogger()
    for h in root.handlers:
        if isinstance(h, RotatingFileHandler) and getattr(h, "baseFilename", None) == os.path.abspath(path):
            return
    handler = RotatingFileHandler(path, maxBytes=max_bytes, backupCount=backup_count, encoding="utf-8")
    handler.setFormatter(logging.Formatter("%(asctime)s [%(levelname)s] %(name)s: %(message)s"))
    root.addHandler(handler)
