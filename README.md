# Bowling Game - Full Stack Application

Vollständige Bowling-Scoring-App mit **Spring Boot Backend** (Scoring-Logik + REST-API) und **Angular Frontend** (moderne Standalone Components).

## Übersicht

Dieses Projekt demonstriert eine Full-Stack-Implementierung des klassischen Bowling-Scoring-Katas:

- **Backend**: Spring Boot 3.3.2 (Java 17)
- **Frontend**:
`frontend_enhancements_angular21/` - **Angular 21 LTS**

---

## Prerequisites

### Backend Requirements

**Java**: Version **17** oder höher 
**Maven**: Version **3.8** oder höher

### Frontend Requirements

**Node.js**: Version **18.x** oder höher

**npm**: Version **9** oder höher 
### Schnelle Installation

<details>
<summary><strong>Ubuntu/Debian</strong></summary>

```bash
# Java 17 installieren
sudo apt update
sudo apt install openjdk-17-jdk

# Maven installieren
sudo apt install maven

# Node.js 20 LTS installieren (via NodeSource)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# Versionen überprüfen
java -version    # openjdk version "17.x.x"
mvn -version     # Apache Maven 3.x.x
node -v          # v20.x.x
npm -v           # 10.x.x
```
</details>

<details>
<summary><strong>macOS</strong></summary>

```bash
# Mit Homebrew (https://brew.sh/)
brew install openjdk@17
brew install maven
brew install node@20

# Versionen überprüfen
java -version
mvn -version
node -v
npm -v
```
</details>

<details>
<summary><strong>Windows</strong></summary>

```powershell
# Mit Chocolatey (https://chocolatey.org/)
choco install openjdk17
choco install maven
choco install nodejs-lts

# Versionen überprüfen
java -version
mvn -version
node -v
npm -v
```
</details>

---

## Schnellstart

### Backend starten (Spring Boot)

```bash
cd backend
mvn spring-boot:run
```

✅ Läuft auf: **http://localhost:8080**
📚 API Docs (Swagger): **http://localhost:8080/swagger-ui.html**

**Verfügbare Endpoints:**
- `GET  /api/bowling/state` – Aktueller Spielstand
- `POST /api/bowling/roll`  – Würfeln (Body: `{ "pins": 7 }`)
- `POST /api/bowling/reset` – Neues Spiel starten

**Tests ausführen:**
```bash
cd backend
mvn test
```
Deckt ab: Perfect Game (300), Gutter Game (0), Spare, Strike, 10. Frame Sonderregeln, Validierung

---

### Frontend starten (Angular 21)

```bash
cd frontend_enhancements_angular21
npm install
npm start
```
✅ Läuft auf: **http://localhost:4201**

> **Hinweis**: Das Frontend kommuniziert mit dem Backend auf `http://localhost:8080` (CORS ist konfiguriert)

---

## Architektur / Design

### Backend-Architektur

- **Single Source of Truth**: Nur die flache Liste aller Rolls (`List<Integer>`)
  wird gehalten. Frames und Scores werden bei jedem Request daraus neu
  berechnet (`BowlingGame.calculateFrames`) – kein doppelter, potentiell
  inkonsistenter State.
- **Unvollstaendige Frames**: Ein Frame, dessen Bonus-Wuerfe (Spare/Strike)
  noch nicht geworfen wurden, wird mit `score: null`, `complete: false`
  zurueckgegeben. Das Frontend zeigt dafuer noch keine Punktzahl an.
- **10. Frame**: wird als Sonderfall behandelt (bis zu 3 Rolls, korrekte
  Regeln fuer Bonus-Wuerfe nach Spare/Strike).
- **Validierung**: Sowohl serverseitig (harte Regel, z.B. "Summe zweier
  Rolls > 10 Pins") als auch clientseitig (Buttons fuer unzulaessige
  Pin-Zahlen werden deaktiviert, fuer bessere Usability).
- **Ein Spieler**: Wie in der Aufgabenstellung ausreichend – ein einziger
  In-Memory-`GameService` pro Backend-Instanz, kein Session-Handling.

### Business Rules

**Aktuelle Implementierung**: Die Business-Logik für Bowling-Regeln (Frame-Typen, Scoring, Bonus-Berechnung) ist **hart kodiert** in `BowlingGame.java` mit if/else und switch-Statements.

**für komplexere, häufig wechselnde Anforderungen **: Bei mehr Zeit/Budget sollte eine **DMN (Decision Model and Notation)** Decision Engine eingesetzt werden:

**Vorteile einer DMN-basierten Lösung:**
- ✅ **Regeländerungen ohne Code-Deployment**: Business-Regeln in DMN-Tabellen änderbar
- ✅ **Lesbarkeit**: Nicht-Entwickler (Fachexperten) können Regeln verstehen und validieren
- ✅ **Testbarkeit**: Regeln können unabhängig vom Code getestet werden
- ✅ **Wartbarkeit**: Komplexe Regellogik wird aus dem Code extrahiert
- ✅ **Versionierung**: DMN-Dateien können separat versioniert werden

---

## Implementierungsschritte

Dieses Projekt wurde in folgenden Schritten implementiert:

### Phase 1: Backend Setup (Spring Boot)

**1. Projekt initialisieren**
```bash
# Spring Initializr (https://start.spring.io/)
# Dependencies: Spring Web, Validation, SpringDoc OpenAPI
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=bowling-backend \
  -DarchetypeArtifactId=maven-archetype-quickstart
```

**2. Core Game Logic implementieren** (`BowlingGame.java`)
- Flache Liste aller Würfe (`List<Integer> rolls`)
- Frame-Berechnung mit Strike/Spare-Logik
- 10. Frame Sonderregeln (bis zu 3 Würfe)
- Score-Berechnung mit Bonus-System

**3. Service Layer erstellen** (`GameService.java`)
- Singleton-Service für Spielverwaltung
- Methoden: `roll()`, `reset()`, `getState()`
- Validierung der Eingaben

**4. REST Controller** (`BowlingController.java`)
- `GET /api/bowling/state` - GameState zurückgeben
- `POST /api/bowling/roll` - Wurf registrieren
- `POST /api/bowling/reset` - Spiel zurücksetzen

**5. DTOs & Validierung**
- `GameState.java` - Response-Objekt mit Frames
- `FrameResult.java` - Frame-Daten
- `RollRequest.java` - Request mit Bean Validation (`@Min`, `@Max`)

**6. CORS konfigurieren** (`CorsConfig.java`)
```java
.allowedOrigins("http://localhost:4200", "http://localhost:4201")
.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
```

**7. Tests schreiben**
- Unit Tests: Perfect Game, Gutter Game, Spares, Strikes
- Edge Cases: 10. Frame, Validierung

---

### Phase 2: Frontend Setup (Angular 21)

**1. Angular Projekt erstellen**
```bash
ng new bowling-frontend-angular21 --standalone --routing=false
cd bowling-frontend-angular21
```

**2. Service Layer** (`bowling.service.ts`)
```typescript
@Injectable({ providedIn: 'root' })
export class BowlingService {
  private baseUrl = 'http://localhost:8080/api/bowling';

  getState(): Observable<GameState> { ... }
  roll(pins: number): Observable<GameState> { ... }
  reset(): Observable<GameState> { ... }
}
```

**3. Component mit Signals** (`app.component.ts`)
```typescript
// Reactive state (Angular 21)
readonly state = signal<GameState | null>(null);
readonly loading = signal(false);

// Computed signals für abgeleitete Werte
readonly currentFrame = computed(() => { ... });
readonly maxPinsForNextRoll = computed(() => { ... });
```

**4. Template** (`app.component.html`)
```html
<!-- @if/@for Control Flow (Angular 17+) -->
@if (state(); as currentState) {
  @for (frame of currentState.frames; track frame.frameNumber) {
    <div class="frame">{{ frame.score }}</div>
  }
}
```

**5. Styling** (`app.component.css`)
- Scoresheet-Layout (Grid/Flexbox)
- Pin-Button-Styling
- Responsive Design

**6. Configuration** (`main.ts`)
```typescript
import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withFetch } from '@angular/common/http';

bootstrapApplication(AppComponent, {
  providers: [provideHttpClient(withFetch())]
});
```

---

### Phase 3: Integration & Testing

**1. CORS testen**
- Frontend auf Port 4201 starten
- Backend auf Port 8080 starten
- API-Kommunikation verifizieren

**2. End-to-End Tests**
- Perfect Game (alle Strikes) → 300 Punkte
- All Spares → 150 Punkte
- Verschiedene Kombinationen

**3. Bug Fixes & Edge Cases**
- `maxPinsForNextRoll` Logik nach Spare
- 10. Frame Bonus-Würfe
- Fehlerbehandlung bei ungültigen Eingaben

**4. Dokumentation**
- README mit Setup-Anleitung
- API-Dokumentation (Swagger)
- Code-Kommentare

---





### Wichtige Design-Entscheidungen

✅ **Single Source of Truth**: Nur `List<Integer> rolls` wird gespeichert, Frames werden berechnet
✅ **Reactive State**: Angular Signals für automatische UI-Updates
✅ **Validierung**: Sowohl Backend (Bean Validation) als auch Frontend (Button-Disabling)
✅ **CORS**: Explizite Konfiguration für lokale Entwicklung
✅ **Zoneless**: Angular 21 ohne Zone.js für bessere Performance
✅ **TypeScript**: Typsicherheit zwischen Frontend und Backend (DTOs)
