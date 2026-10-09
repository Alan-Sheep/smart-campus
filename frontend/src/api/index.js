/**
 * API 接口统一管理
 * 所有接口都从这里统一导出，方便页面调用
 *
 * 使用方式：
 * import { userApi, homeApi } from '@/api/index'
 * 或
 * import userApi from '@/api/user'
 */

// 用户相关
export { userApi } from './user'
export { default as user } from './user'

// 学校相关
export { schoolApi } from './school'
export { default as school } from './school'

// 首页相关
export { homeApi } from './home'
export { default as home } from './home'

// 快递代取
export { expressApi } from './express'
export { default as express } from './express'

// 打印服务
export { printApi } from './print'
export { default as print } from './print'

// 校园跑腿
export { errandApi } from './errand'
export { default as errand } from './errand'

// 代课服务
export { substituteApi } from './substitute'
export { default as substitute } from './substitute'

// 校园活动
export { activityApi } from './activity'
export { default as activity } from './activity'

// 二手市场
export { goodsApi } from './goods'
export { default as goods } from './goods'

// 游戏搭子
export { gameApi } from './game'
export { default as game } from './game'

// 失物招领
export { lostFoundApi } from './lostfound'
export { default as lostfound } from './lostfound'

// 校园频道（帖子）
export { channelApi, postApi } from './channel'
export { default as channel } from './channel'

// 消息相关
export { messageApi } from './message'
export { default as message } from './message'

// 文件上传
export { fileApi, uploadApi } from './file'
export { default as file } from './file'

// 意见反馈
export { feedbackApi } from './feedback'
export { default as feedback } from './feedback'
