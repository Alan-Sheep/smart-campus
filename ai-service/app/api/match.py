from fastapi import APIRouter

from app.models.lost_item import MatchRequest, ApiResponse
from app.services.match_service import MatchService

# 创建路由实例
router = APIRouter()


@router.post("/match", response_model=ApiResponse)
def match_lost_item(request: MatchRequest):
    """
    智能匹配接口
    根据物品信息在对应学校的向量索引中搜索相似物品
    接口路径：POST /api/match
    """
    # 实例化匹配服务
    service = MatchService()

    # 执行匹配
    matches = service.match(request)

    # 返回统一格式的响应
    return ApiResponse(
        code=200,
        message="success",
        data=matches,
    )


@router.post("/index", response_model=ApiResponse)
def index_item(request: MatchRequest):
    """
    将物品加入向量索引
    接口路径：POST /api/index
    """
    # 实例化匹配服务
    service = MatchService()

    # 将物品信息加入索引
    service.add_to_index(request)

    # 返回统一格式的响应
    return ApiResponse(
        code=200,
        message="已加入向量索引",
        data=None,
    )
