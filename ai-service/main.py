import uvicorn
from fastapi import FastAPI

from app.api import match, health

# 创建 FastAPI 应用实例
app = FastAPI(
    title="智慧校园 AI 服务",
    description="提供失物智能匹配等 AI 能力",
    version="1.0.0"
)

# 注册健康检查路由
app.include_router(health.router, tags=["健康检查"])

# 注册智能匹配相关路由，统一前缀为 /api
# 接口路径示例：POST /api/match、POST /api/index
app.include_router(match.router, prefix="/api", tags=["失物智能匹配"])


if __name__ == "__main__":
    # 启动服务，监听 0.0.0.0:8000
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
