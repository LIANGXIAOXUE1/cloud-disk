私人聚合云盘项目 - 完整系统蓝图
一、整体模块划分
1.1 后端模块架构
模块名称    核心职责
auth-module    用户注册/登录、鉴权、token管理
file-module    文件上传/下载/管理、版本控制
storage-module    多存储策略抽象、聚合调度
preview-module    文件在线预览、格式转换
share-module    分享链接生成、提取码校验
transfer-module    百度网盘转存、异步任务调度
search-module    全局搜索、索引管理
recycle-module    回收站、文件彻底删除
1.2 前端模块架构
模块名称    页面/组件
auth-pages    Login、Register、ForgotPassword
main-pages    Dashboard、Home
file-pages    FileManager、FilePreview、Upload
share-pages    ShareList、ShareDetail
transfer-pages    TransferList、AddTask
settings-pages    Profile、StorageBinding、PasswordChange
common-components    UploadDialog、PreviewDialog、Breadcrumb
二、后端核心包结构（com.cloud.disk）
com.cloud.disk
├── config/                          # 全局配置
│   ├── MinioConfig.java            # MinIO本地存储配置
│   ├── RedisConfig.java            # Redis连接与缓存配置
│   ├── RabbitMQConfig.java          # 消息队列配置
│   ├── SaTokenConfig.java          # Sa-Token鉴权配置
│   └── WebMvcConfig.java            # 拦截器与跨域配置
├── common/
│   ├── constant/                   # 常量定义
│   │   ├── StorageType.java        # 存储类型枚举
│   │   ├── FileStatus.java         # 文件状态枚举
│   │   └── ErrorCode.java          # 错误码定义
│   ├── exception/                  # 异常处理
│   │   ├── BusinessException.java  # 业务异常基类
│   │   └── FileException.java      # 文件操作异常
│   ├── result/                     # 统一响应
│   │   ├── Result.java             # 统一返回结构
│   │   └── PageResult.java         # 分页返回结构
│   └── utils/                      # 工具类
│       ├── FileUtils.java          # 文件处理工具
│       ├── IdGenerator.java        # ID生成器
│       └── Md5Utils.java           # MD5工具
├── context/                        # 请求上下文
│   └── UserContext.java            # 当前登录用户信息
├── aspect/                         # AOP切面
│   ├── RateLimitAspect.java       # 限流切面
│   └── LogAspect.java              # 日志切面
├── filter/                         # 过滤器
│   └── AuthFilter.java            # 鉴权过滤器
├── config/                         # 配置（续）
│   └── SecurityConfig.java         # 安全配置
├── controller/                     # 控制层（按模块分包）
│   ├── auth/                       # 认证模块
│   │   ├── AuthController.java    # 登录/注册/登出
│   │   └── UserController.java    # 用户信息/修改密码
│   ├── file/                       # 文件模块
│   │   ├── FileController.java    # 文件CRUD
│   │   ├── UploadController.java  # 上传接口
│   │   └── DownloadController.java # 下载接口
│   ├── storage/                   # 存储模块
│   │   ├── StorageController.java # 存储策略管理
│   │   └── BindingController.java # 存储绑定管理
│   ├── preview/                   # 预览模块
│   │   └── PreviewController.java # 预览地址生成
│   ├── share/                     # 分享模块
│   │   ├── ShareController.java   # 分享链接管理
│   │   └── ExtractController.java # 提取码校验
│   ├── transfer/                  # 转存模块
│   │   ├── TransferController.java # 转存任务管理
│   │   └── BaiduController.java   # 百度网盘接口
│   ├── search/                    # 搜索模块
│   │   └── SearchController.java  # 全局搜索
│   ├── recycle/                   # 回收站模块
│   │   └── RecycleController.java # 回收站操作
│   └── admin/                     # 管理模块（可选）
│       └── AdminController.java   # 管理员接口
├── service/                        # 业务层（按模块分包）
│   ├── impl/                       # 实现类
│   │   ├── auth/
│   │   ├── file/
│   │   ├── storage/
│   │   ├── preview/
│   │   ├── share/
│   │   ├── transfer/
│   │   ├── search/
│   │   └── recycle/
│   └── interface/                  # 接口定义
│       ├── IFileService.java
│       ├── IStorageService.java
│       └── ...
├── repository/                     # 持久层（MyBatis-Plus）
│   ├── mapper/                     # Mapper接口
│   │   ├── UserMapper.java
│   │   ├── FileMapper.java
│   │   ├── StorageBindingMapper.java
│   │   └── ...
│   └── entity/                     # 实体类
│       ├── User.java
│       ├── File.java
│       ├── StorageBinding.java
│       └── ...
├── strategy/                       # 策略模式核心
│   ├── StorageStrategy.java        # 存储策略接口
│   ├── impl/
│   │   ├── AliyunStrategy.java    # 阿里云盘策略
│   │   ├── OneDriveStrategy.java  # OneDrive策略
│   │   ├── Cloud123Strategy.java  # 123云盘策略
│   │   └── MinioStrategy.java     # 本地MinIO策略
│   └── context/
│       └── StorageContext.java    # 策略调度上下文
├── mq/                             # 消息队列
│   ├── consumer/                   # 消费者
│   │   ├── TransferConsumer.java  # 转存任务消费
│   │   └── ThumbnailConsumer.java # 缩略图生成消费
│   └── producer/
│       └── TransferProducer.java # 转存任务生产者
├── schedule/                       # 定时任务
│   └── CleanSchedule.java         # 清理过期分享/临时文件
└── security/                       # 安全相关
    ├── PasswordEncoder.java        # 密码加密
    └── TokenService.java          # Token服务
三、前端页面路由结构
3.1 路由分层结构
// router/index.js - 路由配置
const routes = [
  // ==================== 静态路由（无需鉴权）====================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/share/:shareId',
    name: 'ShareDownload',
    component: () => import('@/views/share/ShareDownload.vue'),
    meta: { title: '分享下载', requiresAuth: false }
  },
  // ==================== 动态路由（需鉴权）====================
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      // ---------- 控制台 ----------
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/main/Dashboard.vue'),
        meta: { title: '控制台', icon: 'HomeFilled' }
      },
      // ---------- 文件管理 ----------
      {
        path: 'files',
        name: 'FileManager',
        component: () => import('@/views/file/FileManager.vue'),
        meta: { title: '我的文件', icon: 'Folder', keepAlive: true }
      },
      {
        path: 'files/preview/:fileId',
        name: 'FilePreview',
        component: () => import('@/views/file/FilePreview.vue'),
        meta: { title: '文件预览', hidden: true }
      },
      {
        path: 'files/upload',
        name: 'UploadCenter',
        component: () => import('@/views/file/UploadCenter.vue'),
        meta: { title: '上传中心', icon: 'Upload' }
      },
      // ---------- 分享管理 ----------
      {
        path: 'shares',
        name: 'ShareList',
        component: () => import('@/views/share/ShareList.vue'),
        meta: { title: '我的分享', icon: 'Share' }
      },
      // ---------- 转存任务 ----------
      {
        path: 'transfer',
        name: 'TransferList',
        component: () => import('@/views/transfer/TransferList.vue'),
        meta: { title: '百度转存', icon: 'Link' }
      },
      {
        path: 'transfer/add',
        name: 'AddTransfer',
        component: () => import('@/views/transfer/AddTransfer.vue'),
        meta: { title: '添加转存任务', hidden: true }
      },
      // ---------- 回收站 ----------
      {
        path: 'recycle',
        name: 'RecycleBin',
        component: () => import('@/views/recycle/RecycleBin.vue'),
        meta: { title: '回收站', icon: 'Delete' }
      },
      // ---------- 搜索 ----------
      {
        path: 'search',
        name: 'GlobalSearch',
        component: () => import('@/views/search/GlobalSearch.vue'),
        meta: { title: '全局搜索', icon: 'Search' }
      },
      // ---------- 个人中心 ----------
      {
        path: 'profile',
        name: 'UserProfile',
        component: () => import('@/views/settings/Profile.vue'),
        meta: { title: '个人资料', icon: 'User' }
      },
      {
        path: 'storage-binding',
        name: 'StorageBinding',
        component: () => import('@/views/settings/StorageBinding.vue'),
        meta: { title: '存储绑定', icon: 'Connection' }
      },
      {
        path: 'change-password',
        name: 'ChangePassword',
        component: () => import('@/views/settings/ChangePassword.vue'),
        meta: { title: '修改密码', hidden: true }
      },
    ]
  },
  // ==================== 404 ============
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue')
  }
]
3.2 页面功能说明
页面路径    组件名
/login    Login
/register    Register
/dashboard    Dashboard
/files    FileManager
/files/preview/:id    FilePreview
/files/upload    UploadCenter
/shares    ShareList
/share/:shareId    ShareDownload
/transfer    TransferList
/transfer/add    AddTransfer
/recycle    RecycleBin
/search    GlobalSearch
/profile    UserProfile
/storage-binding    StorageBinding
/change-password    ChangePassword
四、核心业务数据流
4.1 用户上传文件流程（分片上传+断点续传）
┌─────────────────────────────────────────────────────────────────────────────┐
│                              用户上传文件完整流程                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  【阶段一：初始化上传】                                                       │
│  ┌──────────┐    ┌──────────────┐    ┌─────────────────┐    ┌────────────┐ │
│  │ 前端      │───▶│ UploadController │───▶│ FileService    │───▶│ Redis     │ │
│  │ 选择文件   │    │ /upload/init   │    │ 预生成文件记录  │    │ 保存分片元数据│ │
│  └──────────┘    └──────────────┘    └─────────────────┘    └────────────┘ │
│       │                │                      │                    │        │
│       │         返回uploadId           持久化fileId           记录分片状态   │
│       │                │                      │                    │        │
│  【阶段二：分片上传】                                                         │
│  ┌──────────┐    ┌──────────────┐    ┌─────────────────┐    ┌────────────┐ │
│  │ 前端      │───▶│ UploadController │───▶│ FileService    │───▶│ MinIO      │ │
│  │ 循环分片   │    │ /upload/chunk   │    │ 校验分片序号   │    │ 临时存储   │ │
│  └──────────┘    └──────────────┘    └─────────────────┘    └────────────┘ │
│       │                │                      │                    │        │
│       │         返回已上传分片列表            返回存储路径             │        │
│       │                │                      │                    │        │
│  【阶段三：合并分片】                                                         │
│  ┌──────────┐    ┌──────────────┐    ┌─────────────────┐    ┌────────────┐ │
│  │ 前端      │───▶│ UploadController │───▶│ FileService    │───▶│ MinIO      │ │
│  │ 发送合并   │    │ /upload/merge   │    │ 触发MinIO合并  │    │ 合并分片   │ │
│  └──────────┘    └──────────────┘    └─────────────────┘    └────────────┘ │
│       │                │                      │                    │        │
│       │         返回最终文件信息             更新文件状态             │        │
│       │                │                      │                    │        │
│  【阶段四：存储策略分发】（可选：自动分流到网盘）                              │
│  ┌──────────┐    ┌──────────────┐    ┌─────────────────┐                   │
│  │ FileService│───▶│ StorageContext│───▶│ AliyunStrategy │                   │
│  │ 判断文件大小 │    │ 路由到策略    │    │ 上传到阿里云盘  │                   │
│  └──────────┘    └──────────────┘    └─────────────────┘                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
关键设计点：

1. uploadId：Redis生成唯一标识，关联分片信息，支持页面关闭后续传

2. 分片元数据：Redis缓存分片上传状态（chunkCount、uploadedChunks），合并后清除

3. MinIO临时存储：分片先存本地MinIO，合并成功后删除分片，释放空间

4. 存储分流：大文件（>100MB）自动上传阿里云盘，私密文件留本地MinIO
   4.2 百度网盘转存流程（RabbitMQ异步）
   ┌─────────────────────────────────────────────────────────────────────────────┐
   │                            百度网盘转存完整流程                               │
   ├─────────────────────────────────────────────────────────────────────────────┤
   │                                                                             │
   │  【阶段一：添加转存任务】                                                     │
   │  ┌──────────┐    ┌──────────────┐    ┌─────────────────┐    ┌────────────┐ │
   │  │ 前端      │───▶│ TransferController│───▶│ TransferService│───▶│ MySQL     │ │
   │  │ 链接+提取码│    │ /transfer/add    │    │ 解析百度分享   │    │ 创建任务   │ │
   │  └──────────┘    └──────────────┘    └─────────────────┘    └────────────┘ │
   │       │                │                      │                    │        │
   │       │         返回任务ID                 调用百度API              │        │
   │       │                │                      验证提取码有效性        │        │
   │       │                │                      │                    │        │
   │  【阶段二：投递消息】                                                         │
   │  ┌──────────┐    ┌──────────────┐    ┌─────────────────┐                   │
   │  │TransferService│───▶│ TransferProducer │───▶│ RabbitMQ      │                   │
   │  │              │    │                │    │ 转存队列      │                   │
   │  └──────────┘    └──────────────┘    └─────────────────┘                   │
   │       │                │                      │                           │
   │  消息内容：shareId, extractCode, targetPath, userId                       │
   │                                                                             │
   │  【阶段三：消费转存】（异步处理）                                             │
   │  ┌──────────┐    ┌──────────────┐    ┌─────────────────┐    ┌────────────┐ │
   │  │ RabbitMQ │───▶│TransferConsumer│───▶│ BaiduAPI Service│───│ 存储服务   │ │
   │  │ 消息触发  │    │ 监听转存队列   │    │ 调用百度开放API  │    │ 分发到对应 │ │
   │  └──────────┘    └──────────────┘    └─────────────────┘    └────────────┘ │
   │       │                │                      │                    │        │
   │       │         更新任务状态             下载到本地临时目录           目标存储  │
   │       │         processing              │                    │        │
   │       │                │                      │                    │        │
   │  【阶段四：上传目标存储】                                                     │
   │  ┌──────────┐    ┌──────────────┐    ┌─────────────────┐                   │
   │  │ 转存服务  │───▶│ StorageContext│───▶│ 策略实现类      │                   │
   │  │ 读取临时文件│    │ 路由选择     │    │ Aliyun/Minio  │                   │
   │  └──────────┘    └──────────────┘    └─────────────────┘                   │
   │                                                                             │
   │  【阶段五：回调更新】                                                         │
   │  ┌──────────┐    ┌──────────────┐    ┌─────────────────┐                   │
   │  │ 转存服务  │───▶│ FileService  │───▶│ MySQL          │                   │
   │  │ 转存成功  │    │ 创建文件记录  │    │ 更新任务状态+文件│                   │
   │  └──────────┘    └──────────────┘    └─────────────────┘                   │
   │                                                                             │
   │  【阶段六：通知前端】（WebSocket或轮询）                                      │
   │  ┌──────────┐    ┌──────────────┐                                            │
   │  │ 前端      │◀──│ 状态推送     │                                            │
   │  │ 任务列表   │    │ 完成/失败    │                                            │
   │  └──────────┘    └──────────────┘                                            │
   │                                                                             │
   └─────────────────────────────────────────────────────────────────────────────┘
   关键设计点：

5. 百度API：使用百度网盘开放平台API，需用户授权获取access_token

6. 消息队列：转存为耗时操作（分钟级），必须异步，RabbitMQ保证消息不丢失

7. 重试机制：转存失败自动重试3次，失败进入死信队列，人工处理

8. 批量转存：多个链接逐个入队，前端可查看排队状态
   五、关键接口列表（Controller层）
   5.1 认证模块（auth）
   接口路径
   /api/auth/login
   /api/auth/register
   /api/auth/logout
   /api/auth/getUserInfo
   5.2 用户模块（user）
   接口路径
   /api/user/profile
   /api/user/profile
   /api/user/changePassword
   /api/user/spaceStats
   5.3 文件模块（file）
   接口路径
   /api/file/list
   /api/file/createFolder
   /api/file/rename
   /api/file/move
   /api/file/copy
   /api/file/delete
   /api/file/batchDelete
   /api/file/getDownloadUrl
   5.4 上传模块（upload）
   接口路径
   /api/upload/init
   /api/upload/chunk
   /api/upload/merge
   /api/upload/chunks
   5.5 预览模块（preview）
   接口路径
   /api/preview/getUrl
   /api/preview/getVideoUrl
   5.6 分享模块（share）
   接口路径
   /api/share/create
   /api/share/list
   /api/share/cancel
   /api/share/extract
   /api/share/download
   5.7 转存模块（transfer）
   接口路径
   /api/transfer/add
   /api/transfer/list
   /api/transfer/detail
   /api/transfer/cancel
   /api/transfer/retry
   5.8 搜索模块（search）
   接口路径
   /api/search/global
   /api/search/suggest
   5.9 回收站模块（recycle）
   接口路径
   /api/recycle/list
   /api/recycle/restore
   /api/recycle/permanentDelete
   /api/recycle/clear
   5.10 存储模块（storage）
   接口路径
   /api/storage/binding/list
   /api/storage/binding/add
   /api/storage/binding/remove
   /api/storage/strategy/set
   /api/storage/space/query
   六、存储策略架构
   6.1 策略接口定义
   // 策略接口
   public interface StorageStrategy {
    // 存储类型标识
    StorageType getType();
    // 上传文件
    String upload(InputStream input, String fileName, String path);
    // 下载文件
    InputStream download(String filePath);
    // 删除文件
    boolean delete(String filePath);
    // 获取文件外链（临时/永久）
    String getFileUrl(String filePath, long expireSeconds);
    // 获取剩余空间
    long getAvailableSpace();
    // 文件是否存在
    boolean exists(String filePath);
   }
   6.2 策略实现类
   实现类    存储类型
   MinioStrategy    LOCAL
   AliyunStrategy    ALIYUN
   OneDriveStrategy    ONEDRIVE
   Cloud123Strategy    CLOUD123
   6.3 策略调度上下文
   @Service
   public class StorageContext {
    @Autowired
    private Map<String, StorageStrategy> strategyMap;
    // 根据存储类型获取策略
    public StorageStrategy getStrategy(StorageType type) {
   
        return strategyMap.get(type.name());
   
    }
    // 存储选择算法（自动分流）
    public StorageType selectStorage(FileInfo fileInfo) {
   
        // 大文件 > 100MB → 阿里云盘（不限速）
        // 私密文件标记 → 本地MinIO
        // 小文件 → 就近原则/负载均衡
        // OneDrive教育版 → 空间最大优先
   
    }
   }
   6.4 多存储聚合逻辑
   ┌─────────────────────────────────────────────────────────────────┐
   │                        多存储聚合架构                            │
   ├─────────────────────────────────────────────────────────────────┤
   │                                                                 │
   │  ┌──────────────┐                                               │
   │  │ 用户上传文件  │                                               │
   │  └──────┬───────┘                                               │
   │         │                                                        │
   │         ▼                                                        │
   │  ┌──────────────────────────────────────────────────┐          │
   │  │            StorageContext 策略调度                 │          │
   │  │  ┌────────────────────────────────────────────┐  │          │
   │  │  │  存储选择算法：                              │  │          │
   │  │  │  1. 文件大小 > 100MB → AliyunStrategy       │  │          │
   │  │  │  2. 标记"私密" → MinioStrategy              │  │          │
   │  │  │  3. 小文件 → 按空间占比分配                   │  │          │
   │  │  │  4. 手动指定 → 强制使用指定存储               │  │          │
   │  │  └────────────────────────────────────────────┘  │          │
   │  └──────────────────────┬───────────────────────────┘          │
   │                         │                                        │
   │                         ▼                                        │
   │  ┌────────────┬───────────┬────────────┬───────────┐            │
   │  │ MinioStrategy│AliyunStrategy│OneDriveStrat│Cloud123Strat│   │
   │  │   本地MinIO  │  阿里云盘    │  OneDrive  │  123云盘   │            │
   │  │   私密/小文件 │  大文件/不限速 │  教育版无限  │  备份归档  │            │
   │  └────────────┴───────────┴────────────┴───────────┘            │
   │                                                                 │
   │  ┌──────────────────────────────────────────────────┐          │
   │  │              File表存储类型字段                     │          │
   │  │  +--------+----------+----------+----------+      │          │
   │  │  | fileId | path     | storage  | size     |      │          │
   │  │  | 001    | /docs/a  | ALIYUN   | 200MB    |      │          │
   │  │  | 002    | /photo  | MINIO    | 5MB      |      │          │
   │  │  | 003    | /backup | CLOUD123 | 1GB      |      │          │
   │  │  +--------+----------+----------+----------+      │          │
   │  └──────────────────────────────────────────────────┘          │
   │                                                                 │
   │  ┌──────────────────────────────────────────────────┐          │
   │  │              聚合空间计算逻辑                        │          │
   │  │  totalSpace = Minio剩余 + Aliyun剩余 + OneDrive剩余│          │
   │  │              + Cloud123剩余                        │          │
   │  │  用户感知 = 统一视图，无存储差异                      │          │
   │  └──────────────────────────────────────────────────┘          │
   │                                                                 │
   └─────────────────────────────────────────────────────────────────┘
   6.5 存储绑定流程
   ┌─────────────────────────────────────────────────────────────────┐
   │                      存储绑定时序图                              │
   ├─────────────────────────────────────────────────────────────────┤
   │                                                                 │
   │  用户                         后端                      外部API │
   │  │                            │                             │    │
   │  │  1.点击"绑定阿里云盘"        │                             │    │
   │  │───────────────────────────▶│                             │    │
   │  │                            │                             │    │
   │  │                            │  2.生成OAuth授权URL          │    │
   │  │                            │◀────────────────────────────│    │
   │  │                            │                             │    │
   │  │  3.跳转阿里云授权页         │                             │    │
   │  │───────────────────────────────────────────────────────────│
   │  │                            │                             │    │
   │  │                            │  4.用户授权，返回code        │    │
   │  │◀───────────────────────────────────────────────────────────│
   │  │                            │                             │    │
   │  │  5.前端携带code回调         │                             │    │
   │  │───────────────────────────▶│                             │    │
   │  │                            │                             │    │
   │  │                            │  6.用code换取access_token   │    │
   │  │                            │────────────────────────────▶│
   │  │                            │◀────────────────────────────│    │
   │  │                            │                             │    │
   │  │                            │  7.调用API验证token有效性   │    │
   │  │                            │────────────────────────────▶│    │
   │  │                            │◀────────────────────────────│    │
   │  │                            │                             │    │
   │  │                            │  8.存储binding到MySQL        │    │
   │  │                            │  (token加密存储)            │    │
   │  │                            │                             │    │
   │  │  9.绑定成功，返回存储信息    │                             │    │
   │  │◀───────────────────────────│                             │    │
   │                                                                 │
   └─────────────────────────────────────────────────────────────────┘
   架构总结
   层级    核心组件
   认证层    Sa-Token
   Controller层    REST API
   Service层    业务逻辑
   Repository层    MyBatis-Plus
   策略层    StorageStrategy
   消息层    RabbitMQ
   缓存层    Redis+Caffeine
   前端    Vue3+ElementPlus
   此蓝图覆盖私人聚合云盘全场景，贴合SpringBoot3企业级技术栈，每个模块均可独立扩展，满足开源展示与面试实战双重需求。













✅ 全部完成 — npm run build 零错误通过
变更清单

# 文件    改动

1    styles/global.css    完全重写。主色调 #8FB9A8 薄荷绿莫兰迪色系，背景 #FAFAFA，10px→16px 大圆角，卡片轻柔阴影。全局 Element Plus 覆盖：按钮、输入框、弹窗、表格、标签、下拉菜单全部适配。新增移动端响应式、过渡动画、工具类。
2    index.html    引入 Google Fonts 思源黑体（Noto Sans SC），添加字体加载防闪烁逻辑，viewport-fit=cover 移动端适配。
3    layout/MainLayout.vue    完全重写。侧边栏从深色→白色浅色系，集成 CloudLogo，菜单项薄荷绿高亮+左侧指示条。新增响应式：<768px 移动端抽屉式侧边栏 + 底部5项快捷标签栏（el-drawer + bottom bar），768-1024px 平板侧边栏缩窄。毛玻璃顶栏。
4    views/Login.vue    完全重写。三层渐变背景 + 装饰性模糊色块，毛玻璃卡片，SVG 云盘 LOGO，中文界面，宽松表单间距（46px→48px 按钮），薄荷绿主按钮带悬浮动效。
5    components/CloudLogo.vue    新建。简约线条 SVG 云盘 LOGO（云轮廓 + 磁盘盘片 + 读写头 + 上传箭头），支持 collapsed 折叠模式，配色自动跟随主色调。
6    components/UploadZone.vue    新建。完整拖拽上传组件：拖拽区域（dashed 边框 + 悬浮放大动效），文件队列（缩略图预览、文件类型图标、实时进度条、已上传/总大小），暂停/继续/取消操作，全部上传/全部暂停/清除已完成批量操作，图片自动生成预览缩略图。
7    views/FileList.vue    集成 UploadZone 上传组件。文件图标从 18px→22px 放大，新增文件类型专属图标+颜色+背景：🖼️ 图片粉、🎬 视频蓝紫、📦 压缩包橙、📄 文档蓝、📁 文件夹金。文件名旁显示后缀标签。中文界面。
8    views/Home.vue    统计卡片使用莫兰迪配色（非 CSS 变量），中文界面。
9    views/Share.vue    中文界面。
10    views/Transfer.vue    中文界面。
11    views/Recycle.vue    中文界面。
未改动项（严格遵守约束）

- ❌ 路由 router/index.js — 未动
- ❌ 接口 api/modules/* + request.js — 未动
- ❌ main.js / App.vue — 未动
- ❌ 业务逻辑 — 所有 script setup 保持原有逻辑结构
  启动方式
  cd frontend
  npm run dev     # → http://localhost:5173
