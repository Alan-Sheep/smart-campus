from fastapi import APIRouter

# 创建路由实例
router = APIRouter()


@router.get("/health")
def health_check():
    """
    健康检查接口
    用于监控服务是否正常运行
    """
    return {"status": "ok", "service": "smart-campus-ai"}
