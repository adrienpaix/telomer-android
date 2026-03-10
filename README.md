# Telomer Health — Android Patient App

Application Android pour les patients Telomer Health.

## Stack
- **Kotlin 2.x** + **Jetpack Compose** + **Material 3**
- **Hilt** (DI) + **Retrofit** (API) + **Room** (local DB)
- **AppAuth** (Keycloak PKCE)
- **CameraX** + **ML Kit Barcode** + **ONNX Runtime** (food AI)
- **Health Connect** (Google)

## Architecture
Clean Architecture par module feature (MVVM + Repository pattern).

## Build
```bash
./gradlew assembleDebug
```

## API
Backend: `https://api.telomer.health/api/v1/`
Auth: Keycloak PKCE (`https://auth.telomer.health/realms/telemedicine`)
