from pydantic import BaseModel
from typing import List, Optional, Any


# ==================== 请求模型 ====================

class MatchRequest(BaseModel):
    """智能匹配请求模型"""
    # 物品 ID
    itemId: int
    # 物品标题/名称
    title: str
    # 物品描述
    description: str
    # 物品类型：lost（遗失）/ found（拾到）
    type: str = "lost"
    # 学校 ID，用于数据隔离
    schoolId: int


# ==================== 响应模型 ====================

class MatchItem(BaseModel):
    """匹配结果项"""
    # 匹配到的物品 ID
    itemId: int
    # 匹配到的物品标题
    title: str
    # 相似度（0~1，越大越相似）
    similarity: float


class ApiResponse(BaseModel):
    """统一 API 响应格式"""
    # 状态码，200 表示成功
    code: int = 200
    # 响应消息
    message: str = "success"
    # 响应数据
    data: Any = None
