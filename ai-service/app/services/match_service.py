import os
from typing import List, Optional

from app.core.config import settings
from app.models.lost_item import MatchRequest, MatchItem


class MatchService:
    """
    智能匹配服务
    使用 sentence-transformers 生成文本向量，ChromaDB 做向量存储与相似度搜索
    数据按 schoolId 隔离
    """

    # 类级别的懒加载缓存：sentence-transformers 模型
    _model = None
    # 类级别的懒加载缓存：ChromaDB 客户端
    _chroma_client = None

    def __init__(self):
        """初始化服务，确保数据目录存在"""
        # 确保 ChromaDB 持久化目录存在
        os.makedirs(settings.chroma_persist_path, exist_ok=True)

    # ==================== 懒加载：sentence-transformers 模型 ====================

    @classmethod
    def _get_model(cls):
        """
        懒加载 sentence-transformers 模型
        第一次调用时才加载模型，避免启动时等待太久
        """
        if cls._model is None:
            from sentence_transformers import SentenceTransformer
            # 加载多语言 embedding 模型（支持中文）
            cls._model = SentenceTransformer(settings.embedding_model)
        return cls._model

    # ==================== 懒加载：ChromaDB 客户端与集合 ====================

    @classmethod
    def _get_chroma_client(cls):
        """
        懒加载 ChromaDB 持久化客户端
        """
        if cls._chroma_client is None:
            import chromadb
            cls._chroma_client = chromadb.PersistentClient(path=settings.chroma_persist_path)
        return cls._chroma_client

    def _get_collection(self, school_id: int):
        """
        根据 schoolId 获取对应的 ChromaDB 集合
        每个学校使用独立的 collection，实现数据物理隔离
        collection 名称格式：lost_found_{schoolId}
        """
        client = self._get_chroma_client()
        collection_name = f"lost_found_{school_id}"
        # 获取或创建集合（不使用 ChromaDB 内置的 embedding 函数，自己生成向量）
        collection = client.get_or_create_collection(name=collection_name)
        return collection

    # ==================== 文本向量化 ====================

    def _encode_text(self, text: str) -> List[float]:
        """
        使用 sentence-transformers 将文本转换为向量
        :param text: 输入文本
        :return: 向量列表
        """
        model = self._get_model()
        # encode 返回 numpy 数组，转成 list
        embedding = model.encode(text)
        return embedding.tolist()

    # ==================== 数据写入索引 ====================

    def add_to_index(self, request: MatchRequest) -> None:
        """
        将物品信息加入向量索引
        :param request: 匹配请求（包含物品信息与 schoolId）
        """
        # 获取对应学校的 collection
        collection = self._get_collection(request.schoolId)

        # 拼接文本：标题 + 描述
        text = f"{request.title} {request.description}"

        # 生成向量
        embedding = self._encode_text(text)

        # 写入 ChromaDB（upsert：存在则更新，不存在则插入）
        collection.upsert(
            ids=[str(request.itemId)],
            embeddings=[embedding],
            documents=[text],
            metadatas=[{
                "itemId": request.itemId,
                "title": request.title,
                "description": request.description,
                "type": request.type,
            }]
        )

    # ==================== 相似度匹配 ====================

    def match(self, request: MatchRequest, top_k: int = 5) -> List[MatchItem]:
        """
        根据物品信息在对应学校的索引中做相似度匹配
        :param request: 匹配请求（包含物品信息与 schoolId）
        :param top_k: 返回最相似的前 N 条结果
        :return: 匹配结果列表（按相似度从高到低排序）
        """
        # 获取对应学校的 collection
        collection = self._get_collection(request.schoolId)

        # 拼接查询文本
        query_text = f"{request.title} {request.description}"

        # 生成查询向量
        query_embedding = self._encode_text(query_text)

        # 在 ChromaDB 中查询相似向量
        # 注意：ChromaDB 的 query 默认返回 distances（距离），值越小越相似
        results = collection.query(
            query_embeddings=[query_embedding],
            n_results=top_k + 1,  # 多取一条，排除自身
        )

        matches: List[MatchItem] = []

        # 解析查询结果
        if results.get("ids") and results["ids"][0]:
            ids = results["ids"][0]
            metadatas = results["metadatas"][0]
            distances = results["distances"][0]

            for i in range(len(ids)):
                meta = metadatas[i]
                distance = distances[i]

                # 跳过自身（相同 itemId 的记录）
                item_id = meta.get("itemId")
                if item_id == request.itemId:
                    continue

                # 将距离转换为相似度（0~1，越大越相似）
                # ChromaDB 默认使用余弦距离，范围通常在 0~2 之间
                # 这里使用 1 - distance/2 做归一化，确保在 0~1 之间
                similarity = max(0.0, min(1.0, 1.0 - distance / 2.0))

                matches.append(MatchItem(
                    itemId=item_id,
                    title=meta.get("title", ""),
                    similarity=round(similarity, 4),
                ))

                # 达到 top_k 条就停止
                if len(matches) >= top_k:
                    break

        return matches
