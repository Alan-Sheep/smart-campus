from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """应用配置类，使用 pydantic-settings 管理配置"""

    # 应用名称
    app_name: str = "智慧校园 AI 服务"

    # ChromaDB 持久化存储路径（向量数据保存目录）
    chroma_persist_path: str = "./data/chroma"

    # sentence-transformers 嵌入模型名称
    # paraphrase-multilingual-MiniLM-L12-v2：支持多语言（含中文）的轻量模型
    embedding_model: str = "paraphrase-multilingual-MiniLM-L12-v2"

    # 匹配结果默认返回数量
    match_top_k: int = 5

    class Config:
        # 支持从 .env 文件读取配置
        env_file = ".env"


# 全局配置实例
settings = Settings()
