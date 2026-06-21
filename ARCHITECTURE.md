# BauhausRoute 架構圖

此文件整理 BauhausRoute Android 專案目前的主要模組、資料流與外部服務依賴。

## 系統架構

```mermaid
flowchart LR
    subgraph Users["使用者角色"]
        Farmer["農民"]
        Cleaner["清運團隊"]
        Admin["管理者"]
    end

    subgraph App["Android App"]
        Main["MainActivity<br/>App 入口與流程組裝"]
        Auth["登入 / 註冊流程<br/>帳密登入、Google 登入、角色選擇"]
        Compose["Jetpack Compose UI<br/>首頁、狀態、設定、農地詳情"]
        Nav["Navigation Compose<br/>Home / Status / Settings / Detail"]
        MapPhoto["地圖與照片<br/>osmdroid、Photo Picker、EXIF GPS"]
        WeatherUi["天氣與飛行建議<br/>WeatherSnapshot / FlightAdvice"]
    end

    subgraph Domain["本機邏輯"]
        Profile["FarmerLocalStore<br/>UserRole、FarmerProfile、GoogleAccountProfile"]
        RoutePlanner["RoutePlanner<br/>最近鄰排序、距離計算"]
        RoadRoute["RoadRouteService<br/>OSRM 路線查詢、失敗 fallback"]
        WeatherService["CwaWeatherService<br/>縣市解析、CWA JSON 解析"]
    end

    subgraph Data["Room Database"]
        DB["FarmlandInspectionDatabase<br/>farmland_inspections.db"]
        UserDao["UserDao"]
        FarmerDao["FarmerDao"]
        CleanerDao["CleanerDao"]
        UserTable[("user_accounts")]
        FarmTable[("farmland_inspections")]
        TeamTable[("team_profiles")]
        TaskTable[("cleaner_tasks")]
    end

    subgraph External["外部與裝置服務"]
        Google["Google Identity<br/>Credential Manager / Google ID"]
        CWA["中央氣象署 Open Data API"]
        OSRM["OSRM public routing API"]
        OSM["OpenStreetMap tiles"]
        Location["Android Location Provider"]
        Media["Android Photo Picker / MediaStore"]
    end

    Farmer --> Auth
    Cleaner --> Auth
    Admin --> Auth

    Auth --> Main
    Main --> Compose
    Compose --> Nav
    Compose --> MapPhoto
    Compose --> WeatherUi

    Auth --> Profile
    Auth --> UserDao
    Auth --> Google

    Compose --> FarmerDao
    Compose --> CleanerDao
    Compose --> Profile

    MapPhoto --> RoutePlanner
    RoutePlanner --> RoadRoute
    RoadRoute --> OSRM
    MapPhoto --> OSM
    MapPhoto --> Location
    MapPhoto --> Media

    WeatherUi --> WeatherService
    WeatherService --> CWA

    UserDao --> DB
    FarmerDao --> DB
    CleanerDao --> DB
    DB --> UserTable
    DB --> FarmTable
    DB --> TeamTable
    DB --> TaskTable
```

## 資料流

```mermaid
sequenceDiagram
    actor User as 使用者
    participant UI as Compose UI
    participant Auth as 登入流程
    participant DB as Room DAO
    participant Device as 裝置服務
    participant Weather as CwaWeatherService
    participant Route as RoadRouteService
    participant API as 外部 API

    User->>Auth: 登入、Google 登入或註冊角色
    Auth->>DB: 建立或查詢 user_accounts / team_profiles
    Auth-->>UI: 進入主畫面與角色狀態
    User->>UI: 新增農地巡檢或查看清運路線
    UI->>Device: 讀取照片、EXIF GPS、裝置定位
    Device-->>UI: URI、座標或位置 fallback
    UI->>DB: 儲存巡檢、任務、照片 URI
    UI->>Weather: 用地址或座標取得天氣
    Weather->>API: 呼叫中央氣象署 Open Data
    API-->>Weather: 天氣 JSON
    Weather-->>UI: WeatherSnapshot
    UI->>Route: 依優先任務規劃清運順序
    Route->>API: 呼叫 OSRM
    API-->>Route: 路線 GeoJSON
    Route-->>UI: RoadRouteResult
```

## 主要檔案職責

| 檔案 | 職責 |
| --- | --- |
| `app/src/main/java/com/example/bauhausroute/MainActivity.kt` | App 入口、Compose UI、登入流程、導覽、地圖、照片、定位與畫面狀態 |
| `app/src/main/java/com/example/bauhausroute/FarmlandInspectionDatabase.kt` | Room entities、DAO、資料庫 singleton 與 migration |
| `app/src/main/java/com/example/bauhausroute/FarmerLocalStore.kt` | 使用者角色與登入後 profile model |
| `app/src/main/java/com/example/bauhausroute/RoutePlanner.kt` | 清運點最近鄰排序與座標距離計算 |
| `app/src/main/java/com/example/bauhausroute/RoadRouteService.kt` | 呼叫 OSRM、解析路線、路線失敗時 fallback |
| `app/src/main/java/com/example/bauhausroute/CwaWeatherService.kt` | 呼叫中央氣象署 Open Data、解析天氣資料 |

## 圖片檔

架構圖圖片已輸出為：

```text
bauhausroute-architecture.svg
```

可在 README 中引用：

```md
![BauhausRoute 架構圖](./bauhausroute-architecture.svg)
```
