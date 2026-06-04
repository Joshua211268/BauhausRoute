# BauhausRoute 架構圖

```mermaid
flowchart TB
    farmer["農民使用者"]
    cleaner["清運團隊"]
    admin["管理者"]

    subgraph app["Android App: BauhausRoute"]
        main["MainActivity"]
        auth["登入 / 註冊流程<br/>FarmerAuthApp"]
        ui["Jetpack Compose UI<br/>首頁、狀態、設定、農地詳情"]
        nav["Navigation Compose<br/>Home / Status / Settings / Detail"]
        map["地圖與路線畫面<br/>BauhausRouteMap / MapPreview"]
        photo["照片與定位處理<br/>Photo Picker / EXIF GPS / LocationManager"]
        weatherState["天氣與飛行建議<br/>WeatherSnapshot / FlightAdvice"]
    end

    subgraph domain["本機邏輯"]
        localProfile["FarmerLocalStore<br/>UserRole / FarmerProfile / GoogleAccountProfile"]
        routePlanner["RoutePlanner<br/>最近鄰排序 / 距離計算"]
        roadRoute["RoadRouteService<br/>OSRM 路線查詢與 fallback"]
        weatherService["CwaWeatherService<br/>縣市解析 / 天氣 API 解析"]
    end

    subgraph data["Room Database: farmland_inspections.db"]
        db["FarmlandInspectionDatabase"]
        userDao["UserDao"]
        farmerDao["FarmerDao"]
        cleanerDao["CleanerDao"]
        accounts[("user_accounts")]
        farmlands[("farmland_inspections")]
        teams[("team_profiles")]
        tasks[("cleaner_tasks")]
    end

    subgraph external["外部與裝置服務"]
        google["Google Identity<br/>Credential Manager / Google ID"]
        cwa["中央氣象署 Open Data API"]
        osrm["OSRM public routing API"]
        osm["OpenStreetMap tiles<br/>osmdroid"]
        androidLocation["Android Location Provider"]
        mediaStore["Android Photo Picker / MediaStore"]
    end

    farmer --> auth
    cleaner --> auth
    admin --> auth

    main --> auth
    main --> ui
    ui --> nav
    ui --> map
    ui --> photo
    ui --> weatherState

    auth --> localProfile
    auth --> userDao
    auth --> google

    ui --> farmerDao
    ui --> cleanerDao
    ui --> localProfile

    map --> routePlanner
    map --> roadRoute
    map --> osm
    photo --> androidLocation
    photo --> mediaStore
    weatherState --> weatherService

    roadRoute --> osrm
    weatherService --> cwa

    userDao --> db
    farmerDao --> db
    cleanerDao --> db
    db --> accounts
    db --> farmlands
    db --> teams
    db --> tasks
```

## 分層說明

| 層級 | 主要檔案 / 元件 | 職責 |
| --- | --- | --- |
| 使用者入口 | `MainActivity.kt` | App 入口、登入狀態、角色流程與主要 Compose 畫面組裝 |
| UI 與導覽 | `MainActivity.kt`、Navigation Compose | 農民 / 清運角色畫面、底部導覽、農地詳情、地圖與設定頁 |
| 本機資料 | `FarmlandInspectionDatabase.kt` | Room entities、DAO、資料庫 migration、本機帳號與任務資料 |
| 帳號模型 | `FarmerLocalStore.kt` | 使用者角色、農民 profile、Google 帳號 profile |
| 路線規劃 | `RoutePlanner.kt`、`RoadRouteService.kt` | 清運點排序、距離計算、OSRM 路線查詢，失敗時回退成直線路徑 |
| 天氣資料 | `CwaWeatherService.kt` | 依地址或 GPS 推估縣市，呼叫中央氣象署 Open Data 並轉成 App 天氣模型 |
| 裝置能力 | Android Location、Photo Picker、EXIF | 取得位置、照片 URI、照片 GPS 資訊與本機媒體讀取權限 |
| 外部服務 | Google Identity、OSRM、OpenStreetMap、CWA | Google 登入、道路路線、地圖圖資、天氣資料 |

## 資料流摘要

```mermaid
sequenceDiagram
    actor User as 使用者
    participant UI as Compose UI
    participant DB as Room DAO
    participant Media as Photo / Location
    participant Weather as CwaWeatherService
    participant Route as RoadRouteService
    participant External as 外部 API

    User->>UI: 登入、註冊或進入角色畫面
    UI->>DB: 讀寫帳號、農地、清運任務
    User->>UI: 新增巡檢照片或任務
    UI->>Media: 讀取照片、EXIF GPS、裝置定位
    Media-->>UI: URI 與座標
    UI->>DB: 儲存巡檢 / 任務資料
    UI->>Weather: 用地址或座標取得天氣
    Weather->>External: 呼叫中央氣象署 Open Data
    External-->>Weather: 天氣 JSON
    Weather-->>UI: WeatherSnapshot
    UI->>Route: 規劃清運順序與道路路徑
    Route->>External: 呼叫 OSRM
    External-->>Route: 路線 GeoJSON
    Route-->>UI: RoadRouteResult
```
