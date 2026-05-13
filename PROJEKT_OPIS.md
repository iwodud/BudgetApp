# BudgetApp – Opis projektu

## 1. Co robi aplikacja?

BudgetApp to aplikacja Android do zarządzania budżetem osobistym. Działa w pełni offline (bez serwera, bez internetu). Użytkownik może:

- Dodawać transakcje: wydatki, przychody, odkładanie na oszczędności, pobieranie z oszczędności
- Planować budżet miesięczny per kategoria (ile chcę wydać na jedzenie, transport itp.)
- Zarządzać "skarbonkami" – osobnymi celami oszczędnościowymi (np. Wakacje, Fundusz awaryjny)
- Filtrować i przeglądać historię transakcji po miesiącu, kategorii, typie, kwocie
- Oglądać statystyki wydatków z wykresem kołowym
- Eksportować dane do pliku Excel (.xlsx) – historia miesięczna, podsumowanie kategorii, raport roczny
- Przełączać tryb ciemny/jasny

---

## 2. Architektura – poziom MAKRO

Projekt stosuje **Clean Architecture** podzieloną na 3 warstwy:

```
┌─────────────────────────────────┐
│      PRESENTATION (UI)          │  ← Composable, ViewModel
├─────────────────────────────────┤
│      DOMAIN (logika biznesowa)  │  ← modele danych, interfejsy repozytoriów
├─────────────────────────────────┤
│      DATA (dostęp do danych)    │  ← Room, implementacje repozytoriów
└─────────────────────────────────┘
```

**Zasada zależności:** każda warstwa zna tylko warstwę poniżej. UI zna domain, data zna domain. UI nie zna data bezpośrednio.

Wzorzec projektowy: **MVVM** (Model–View–ViewModel)
- **View** = ekrany Compose (`*Screen.kt`)
- **ViewModel** = logika stanu ekranu (`*ViewModel.kt`)
- **Model** = dane z repozytoriów

---

## 3. Stack technologiczny

| Technologia | Rola |
|---|---|
| Kotlin 2.2 | język programowania |
| Jetpack Compose | deklaratywne UI (nie XML) |
| Room 2.7 | baza danych SQLite z ORM |
| KAPT | procesor adnotacji dla Room |
| Navigation Compose | nawigacja między ekranami |
| StateFlow / Flow | reaktywny przepływ danych |
| Coroutines | asynchroniczność (suspend functions) |
| Material 3 | komponenty UI i theming |
| FileProvider | bezpieczne udostępnianie pliku xlsx |

---

## 4. Struktura pakietów – poziom MEZO

```
com.example.budgetapp/
│
├── BudgetApp.kt              ← Application class (punkt startowy procesu)
├── AppContainer.kt           ← ręczny Dependency Injection
│
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt    ← definicja bazy Room (singleton)
│   │   ├── DatabaseSeeder.kt ← wypełnianie przykładowymi danymi przy 1. uruchomieniu
│   │   ├── dao/              ← interfejsy zapytań SQL (Room generuje implementację)
│   │   │   ├── TransactionDao.kt
│   │   │   ├── CategoryDao.kt
│   │   │   ├── BudgetPlanDao.kt
│   │   │   └── SavingsJarDao.kt
│   │   └── entity/           ← klasy mapowane na tabele bazy danych
│   │       ├── TransactionEntity.kt
│   │       ├── CategoryEntity.kt
│   │       ├── BudgetPlanEntity.kt
│   │       └── SavingsJarEntity.kt
│   └── repository/           ← implementacje repozytoriów
│
├── domain/
│   ├── model/                ← czyste modele danych (bez Room, bez Android)
│   │   ├── Transaction.kt
│   │   ├── Category.kt
│   │   ├── BudgetPlan.kt
│   │   ├── SavingsJar.kt
│   │   ├── TransactionType.kt  ← enum: EXPENSE, INCOME, SAVE_TO_JAR, WITHDRAW_FROM_JAR
│   │   └── CategoryType.kt     ← enum: EXPENSE, INCOME
│   └── repository/           ← interfejsy repozytoriów (kontrakty)
│
├── presentation/
│   ├── navigation/
│   │   ├── Screen.kt         ← definicja tras nawigacji
│   │   └── NavGraph.kt       ← kompozycja ekranów + bottom navigation bar
│   ├── common/
│   │   ├── FormatUtils.kt    ← formatowanie kwot, dat, miesięcy
│   │   └── XlsxWriter.kt    ← generator plików Excel (ZipOutputStream)
│   ├── dashboard/            ← ekran główny
│   ├── transactions/         ← lista transakcji + dodawanie/edycja
│   ├── statistics/           ← wykres kołowy + eksport
│   ├── budget/               ← planowanie budżetu
│   ├── savings/              ← skarbonki
│   └── settings/             ← kategorie + dark mode
│
├── ui/theme/
│   ├── Theme.kt              ← kolory jasny/ciemny, MaterialTheme
│   └── Type.kt               ← typografia
│
└── MainActivity.kt           ← jedyna Activity, hostuje cały Compose
```

---

## 5. Przepływ danych – poziom MIKRO

### Jak dane płyną od bazy do ekranu?

```
SQLite (Room)
    ↓  Flow<List<Entity>>  (DAO)
    ↓  map { entity.toDomain() }  (Repository)
    ↓  Flow<List<DomainModel>>
    ↓  combine(...) / collect  (ViewModel)
    ↓  MutableStateFlow<UiState>
    ↓  collectAsState()  (Composable)
    ↓  rekomposition UI
```

**Flow** to strumień danych – Room automatycznie emituje nową wartość kiedy dane w bazie się zmienią. Dzięki temu UI jest zawsze aktualne bez ręcznego odświeżania.

### Przykład konkretny – DashboardScreen:

1. `DashboardViewModel.init` wywołuje `combine(transactionRepo.getTransactionsByMonth(), categoryRepo.getAllCategories(), budgetPlanRepo.getBudgetPlansByMonth())`
2. `combine` czeka na wszystkie 3 strumienie i oblicza `DashboardState` (suma wydatków, przychodów, ostatnie 8 transakcji)
3. `DashboardScreen` robi `val state by viewModel.state.collectAsState()`
4. Compose automatycznie przerysowuje ekran gdy `state` się zmieni

---

## 6. Baza danych

4 tabele Room:

| Tabela | Kolumny kluczowe |
|---|---|
| `transactions` | id, type (String), amount, category_id, savings_jar_id, date, created_at |
| `categories` | id, name, type, is_default, color_hex |
| `budget_plans` | id, category_id, month (format "YYYY-MM"), planned_amount |
| `savings_jars` | id, name, current_amount, goal_amount |

**Dlaczego `type` jako String a nie enum?** Room nie obsługuje natywnie enumów – przechowujemy `TransactionType.name` (np. `"EXPENSE"`) i przy odczycie robimy `TransactionType.valueOf(type)`.

**Dlaczego data jako `Long`?** Timestamp milliseconds od epoch – standardowe podejście w Android, niezależne od strefy czasowej przy zapisie.

---

## 7. Dependency Injection (ręczne)

Projekt nie używa Hilt/Dagger. Zamiast tego:

```
BudgetApp (Application class)
    └── AppContainer (lazy)
            ├── AppDatabase (singleton)
            ├── TransactionRepository
            ├── CategoryRepository
            ├── BudgetPlanRepository
            └── SavingsJarRepository
```

Każdy ViewModel dostaje repozytorium przez `companion object factory(container)`:

```kotlin
// W ekranie:
val viewModel: DashboardViewModel = viewModel(
    factory = DashboardViewModel.factory(app.container)
)
```

---

## 8. Nawigacja

Bottom Navigation Bar z 4 zakładkami: Start / Transakcje / Statystyki / Ustawienia

Ekrany "push" (otwierane na stosie nawigacyjnym):
- Plan budżetu (z Dashboardu)
- Oszczędności (z Dashboardu)
- Dodaj/Edytuj transakcję (z każdego miejsca)

`AddEditTransaction` przyjmuje argument `transactionId: Long` – jeśli `-1`, to nowa transakcja; jeśli > 0, to edycja istniejącej.

---

## 9. Eksport Excel (XlsxWriter)

Format `.xlsx` to tak naprawdę plik ZIP z plikami XML w środku (standard OpenXML). `XlsxWriter.kt` tworzy go od zera używając tylko `ZipOutputStream` z biblioteki standardowej Javy – bez żadnej zewnętrznej biblioteki do excela.

Struktura pliku xlsx wewnątrz:
```
[Content_Types].xml
_rels/.rels
xl/workbook.xml
xl/sharedStrings.xml   ← teksty (żeby nie duplikować)
xl/styles.xml
xl/worksheets/sheet1.xml
```

Plik jest zapisywany do katalogu cache aplikacji i udostępniany innym aplikacjom przez `FileProvider` (mechanizm bezpieczeństwa Android – aplikacje nie mogą bezpośrednio czytać plików innych aplikacji).

---

## 10. Pytania, jakie może zadać prowadzący

**O architekturze:**
- *Dlaczego Clean Architecture?* – separacja odpowiedzialności, testowalność (domain nie zależy od Androida), łatwość zmiany np. bazy danych bez dotykania UI
- *Co daje warstwa domain?* – modele i kontrakty niezależne od frameworka; można testować logikę biznesową bez instrumentacji Androida
- *Dlaczego MVVM a nie MVC/MVP?* – ViewModel przeżywa rotację ekranu, StateFlow jest reaktywny i naturalne dla Compose

**O Room:**
- *Co to jest DAO?* – Data Access Object, interfejs opisujący operacje na bazie; Room generuje implementację w czasie kompilacji (stąd KAPT)
- *Co to jest KAPT?* – Kotlin Annotation Processing Tool; Room czyta adnotacje (@Entity, @Dao, @Database) i generuje kod Java/Kotlin
- *Dlaczego Flow zamiast LiveData?* – Flow jest częścią Kotlin Coroutines, działa poza Androidem (testowalność), lepiej komponuje się z `combine`/`flatMapLatest`

**O Compose:**
- *Co to jest rekomposition?* – Compose przerysowuje tylko te fragmenty UI, których dane się zmieniły; stan jest przechowywany jako `State<T>`
- *Dlaczego `collectAsState()`?* – konwertuje `StateFlow` (coroutines) na `State<T>` (Compose), dzięki czemu zmiana wartości wyzwala rekomposition
- *Co to jest `remember`?* – przechowuje wartość między rekomposycjami; bez tego stan byłby resetowany przy każdym przerysowaniu

**O bezpieczeństwie/Androidzie:**
- *Dlaczego FileProvider?* – od Androida 7.0 aplikacje nie mogą udostępniać pliku bezpośrednio przez ścieżkę; FileProvider generuje bezpieczne URI z czasowym uprawnieniem do odczytu
- *Dlaczego singleton dla bazy danych?* – Room nie jest thread-safe przy tworzeniu; jeden obiekt bazy na cały proces zapobiega konfliktom i jest efektywniejszy

**O wzorcach:**
- *Skąd ViewModel wie o zmianach w bazie?* – Room zwraca `Flow`, który emituje nową wartość przy każdej modyfikacji tabeli; ViewModel subskrybuje ten strumień i aktualizuje `StateFlow`
- *Jak działa combine?* – czeka aż wszystkie wejściowe Flow wyemitują wartość, potem przy każdej zmianie dowolnego z nich wywołuje blok transformacji z najnowszymi wartościami wszystkich
- *Co to jest suspend function?* – funkcja, która może być wstrzymana (nie blokuje wątku); Room wymaga suspend dla operacji zapisu, żeby nie blokować głównego wątku UI
