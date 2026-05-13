# BudgetApp – Opis projektu

## Co robi aplikacja?

Aplikacja Android do zarządzania budżetem osobistym. Działa w 100% offline – bez serwera, bez internetu, dane są tylko na telefonie. Użytkownik może:

- Dodawać wydatki i przychody przypisane do kategorii (np. Jedzenie, Transport)
- Zaplanować ile chce wydać w danym miesiącu per kategoria (budżet miesięczny)
- Odkładać pieniądze do "skarbonki" (np. Wakacje, Fundusz awaryjny) i z niej wypłacać
- Przeglądać historię transakcji z filtrami (miesiąc, kategoria, typ, kwota)
- Oglądać statystyki wydatków z wykresem kołowym
- Eksportować dane do pliku Excel (.xlsx)
- Przełączać ciemny/jasny motyw

---

## Architektura – jak to jest zbudowane ogólnie

Projekt stosuje **Clean Architecture** – to po prostu podział kodu na 3 osobne "piętry":

```
┌─────────────────────────────────────────┐
│            PRESENTATION                 │
│    ekrany Compose + ViewModele          │
├─────────────────────────────────────────┤
│               DOMAIN                    │
│   modele danych + kontrakty repo        │
├─────────────────────────────────────────┤
│                DATA                     │
│    Room + implementacje repo            │
└─────────────────────────────────────────┘
```

**Zasada:** każde piętro rozmawia tylko z piętrem poniżej. UI nie wie jak działa baza danych – wie tylko, że może poprosić repozytorium o dane.

Wzorzec: **MVVM** (Model–View–ViewModel)
- **View** = ekrany Compose (to co widać)
- **ViewModel** = logika ekranu, trzyma stan
- **Model** = dane z repozytoriów

---

## Struktura projektu – 20 plików Kotlin

```
com.example.budgetapp/
│
├── MainActivity.kt              ← jedyna Activity, punkt startowy UI
├── BudgetApp.kt                 ← Application class, zyje przez caly proces
├── AppContainer.kt              ← reczny Dependency Injection
│
├── data/
│   ├── local/
│   │   ├── Database.kt          ← definicja bazy Room + seeder kategorii
│   │   ├── dao/
│   │   │   └── Daos.kt          ← 4 interfejsy DAO (zapytania SQL)
│   │   └── entity/
│   │       └── Entities.kt      ← 4 klasy mapowane na tabele SQLite
│   └── repository/
│       └── Repositories.kt      ← 4 implementacje repozytoriow
│
├── domain/
│   ├── model/
│   │   └── Models.kt            ← czyste modele + enums (bez Androida)
│   └── repository/
│       └── Repositories.kt      ← 4 interfejsy repozytoriow (kontrakty)
│
├── presentation/
│   ├── navigation/
│   │   └── Navigation.kt        ← Screen routes + NavGraph + BottomBar
│   ├── common/
│   │   ├── FormatUtils.kt       ← formatowanie kwot, dat, miesiecy
│   │   └── XlsxWriter.kt        ← generator plikow Excel (.xlsx)
│   ├── dashboard/
│   │   └── Dashboard.kt         ← ekran glowny + ViewModel
│   ├── transactions/
│   │   ├── Transactions.kt      ← lista transakcji + ViewModel
│   │   └── AddEditTransaction.kt← formularz dodawania/edycji + ViewModel
│   ├── statistics/
│   │   └── Statistics.kt        ← statystyki + wykres kolowy + ViewModel
│   ├── budget/
│   │   └── Budget.kt            ← plan budzetu + ViewModel
│   ├── savings/
│   │   └── Savings.kt           ← skarbonki + ViewModel
│   └── settings/
│       └── Settings.kt          ← ustawienia + kategorie + ViewModel
│
└── ui/theme/
    └── Theme.kt                 ← kolory (jasny/ciemny) + typografia
```

---

## Omówienie każdego pliku z osobna

Projekt ma dokładnie **20 plików Kotlin**. Oto co robi każdy z nich:

---

### MainActivity.kt
**Co to jest:** Jedyna Activity w całej aplikacji. Activity to "okno" Androida – bez niej aplikacja nie istnieje. Można myśleć o niej jak o ramce obrazu: sama nic nie pokazuje, ale bez niej nie byłoby gdzie powiesić obrazu.

**Co robi:** Wywołuje `enableEdgeToEdge()` (żeby aplikacja rysowała pod paskiem stanu), potem odpala Compose i wyświetla `AppNavGraph`. Trzyma też stan `isDarkMode` – jeden boolean decydujący o motywie.

---

### BudgetApp.kt
**Co to jest:** Klasa Application. Istnieje przez cały czas życia procesu aplikacji – uruchamia się zanim cokolwiek innego, i żyje do momentu zamknięcia aplikacji.

**Co robi:** Inicjalizuje `AppContainer` (patrz niżej). Dzięki temu baza danych i repozytoria są gotowe zanim którykolwiek ekran ich potrzebuje.

---

### AppContainer.kt
**Co to jest:** Ręcznie napisany "pojemnik na zależności". Zamiast używać biblioteki jak Hilt czy Dagger, wszystko jest tu sklejone ręcznie.

**Co robi:** Tworzy bazę danych (`AppDatabase`) i na jej podstawie tworzy 4 repozytoria. Każdy ViewModel dostaje repozytorium właśnie stąd. Analogia: to jak szatnia na siłowni – każdy (ViewModel) przychodzi, bierze swoje rzeczy (repozytorium) i idzie pracować.

---

### data/local/Database.kt
**Co to jest:** Dwa elementy w jednym pliku:
1. `AppDatabase` – definicja bazy danych SQLite (poprzez Room)
2. `DatabaseSeeder` – kod który przy pierwszym uruchomieniu wstawia do bazy domyślne kategorie (Jedzenie, Transport, itp.)

**Co robi:** `AppDatabase` to singleton – jeden obiekt bazy na całą aplikację, stworzony metodą `Room.databaseBuilder(...)`. Room na podstawie tej klasy generuje automatycznie cały kod obsługi SQLite.

---

### data/local/entity/Entities.kt
**Co to jest:** Cztery klasy danych oznaczone `@Entity` – każda mapuje się na jedną tabelę w bazie SQLite.

**Co robi:** Definiuje strukturę tabel:
- `TransactionEntity` → tabela `transactions` (id, typ, kwota, kategoria, data...)
- `CategoryEntity` → tabela `categories` (id, nazwa, typ, kolor...)
- `BudgetPlanEntity` → tabela `budget_plans` (id, kategoria, miesiąc, planowana kwota)
- `SavingsJarEntity` → tabela `savings_jars` (id, nazwa, aktualna kwota, cel)

Każda entity ma też funkcję rozszerzającą `toDomain()` – konwertuje "surowy" wiersz z bazy na "czysty" obiekt domenowy.

---

### data/local/dao/Daos.kt
**Co to jest:** Cztery interfejsy DAO (Data Access Object). Każdy definiuje zapytania SQL dla jednej tabeli – ale zamiast pisać SQL ręcznie, używamy adnotacji Room.

**Co robi:** Przykład:
```kotlin
@Query("SELECT * FROM transactions WHERE strftime('%Y-%m', datetime(date/1000,'unixepoch')) = :month")
fun getTransactionsByMonth(month: String): Flow<List<TransactionEntity>>
```
Room widzi tę adnotację i **automatycznie generuje implementację** (kod który faktycznie odpytuje SQLite). Stąd potrzebny jest KAPT (procesor adnotacji).

---

### data/repository/Repositories.kt
**Co to jest:** Cztery klasy implementujące interfejsy repozytoriów z warstwy domain. To "most" między DAO (surowe wiersze bazy) a resztą aplikacji (czyste modele).

**Co robi:** Każda metoda pobiera dane z DAO, konwertuje entity na model domenowy (`entity.toDomain()`), i zwraca Flow lub wykonuje operację. UI nigdy nie widzi `Entity` – widzi tylko czyste modele z warstwy domain.

---

### domain/model/Models.kt
**Co to jest:** Czyste modele danych – "prawda" o tym jak wyglądają dane w aplikacji. Nie ma tu ani jednego importu z pakietu Android ani Room.

**Co robi:** Definiuje:
- `Transaction`, `Category`, `BudgetPlan`, `SavingsJar` – data classy z polami
- `TransactionType` – enum: `EXPENSE`, `INCOME`, `SAVE_TO_JAR`, `WITHDRAW_FROM_JAR`
- `CategoryType` – enum: `EXPENSE`, `INCOME`

Dlaczego osobne od Entity? Bo Entity to "jak baza widzi dane", a Model to "jak aplikacja widzi dane". Jeśli zmienimy bazę danych na inną, modele domenowe zostają bez zmian.

---

### domain/repository/Repositories.kt
**Co to jest:** Cztery interfejsy – kontrakty opisujące co można zrobić z danymi. To "menu" usług: "mogę pobrać transakcje, mogę dodać transakcję, mogę usunąć transakcję".

**Co robi:** Definiuje co każde repozytorium musi umieć, ale **nie jak to robi**. Implementacja jest w `data/repository/`. Dzięki temu ViewModel zależy tylko od interfejsu, nie od konkretnej klasy – można podmienić implementację (np. zamiast Room użyć sieci) bez zmiany ViewModela.

---

### presentation/navigation/Navigation.kt
**Co to jest:** Dwa elementy w jednym pliku:
1. `Screen` – sealed class z adresami ekranów (jak URL-e w aplikacji webowej)
2. `AppNavGraph` – cały graf nawigacji + pasek dolny (bottom navigation)

**Co robi:** `AppNavGraph` to jeden duży Scaffold z NavigationBar na dole i NavHost w środku. NavHost wie, że gdy adres to `"dashboard"` – pokaż `DashboardScreen`, gdy `"transactions"` – pokaż `TransactionListScreen`, itd. `AddEditTransaction` przyjmuje argument `transactionId` – jeśli `-1`, to nowa transakcja; jeśli inne, to edycja.

---

### presentation/common/FormatUtils.kt
**Co to jest:** Zbiór funkcji pomocniczych do formatowania.

**Co robi:**
- `formatAmount(1234.5)` → `"1 234,50 zł"`
- `formatDate(timestamp)` → `"13.05.2026"`
- `currentMonth()` → `"2026-05"`
- `previousMonth("2026-05")` → `"2026-04"`
- `monthToDisplay("2026-05")` → `"maj 2026"`

---

### presentation/common/XlsxWriter.kt
**Co to jest:** Generator plików Excel napisany od zera, bez żadnej zewnętrznej biblioteki.

**Co robi:** Format `.xlsx` to ZIP z plikami XML w środku. `XlsxWriter` tworzy ten ZIP ręcznie używając `ZipOutputStream` z Javy. Tworzy plik w katalogu cache aplikacji, a następnie udostępnia go przez `FileProvider` (mechanizm bezpieczeństwa Android) – dzięki temu użytkownik może go otworzyć w Sheets/Excelu.

---

### presentation/dashboard/Dashboard.kt
**Co to jest:** Ekran główny aplikacji. Zawiera ViewModel i ekran w jednym pliku.

**Co robi:**
- `DashboardViewModel` – pobiera dane z 3 repozytoriów naraz używając `combine(...)`, oblicza sumę wydatków/przychodów/bilans dla bieżącego miesiąca
- `DashboardScreen` – wyświetla: kafelki Przychody/Wydatki, bilans miesiąca, pasek postępu budżetu, ostatnie 8 transakcji, przyciski do Budżetu i Oszczędności
- `SummaryCard`, `BudgetProgressCard` – pomocnicze kompozycje

---

### presentation/transactions/Transactions.kt
**Co to jest:** Lista transakcji. Zawiera ViewModel, ekran, i wspólne kompozycje używane też przez Dashboard.

**Co robi:**
- `TransactionListViewModel` – filtruje transakcje po miesiącu, kategorii, typie
- `TransactionListScreen` – lista z filtrowaniem, możliwość usunięcia transakcji (z potwierdzeniem)
- `MonthSelector` – strzałki ← / → do przełączania miesięcy (używany też w Statystykach)
- `TransactionItem` – jeden wiersz transakcji (używany też na Dashboardzie)

---

### presentation/transactions/AddEditTransaction.kt
**Co to jest:** Formularz dodawania i edycji transakcji.

**Co robi:**
- `AddEditTransactionViewModel` – ładuje istniejącą transakcję (jeśli edycja), zapisuje nową lub aktualizuje
- `AddEditTransactionScreen` – formularz z: wyborem typu transakcji, kwotą, kategorią, opisem, datą (DatePickerDialog), wyborem skarbonki (dla SAVE_TO_JAR / WITHDRAW_FROM_JAR)

---

### presentation/statistics/Statistics.kt
**Co to jest:** Ekran statystyk wydatków.

**Co robi:**
- `StatisticsViewModel` – grupuje wydatki po kategoriach, oblicza procenty, liczy największą kategorię i średni wydatek
- `StatisticsScreen` – wyświetla: selektor miesiąca, 4 kafelki ze statystykami, wykres kołowy, listę kategorii z procentami i kwotami, menu eksportu (3 rodzaje pliku xlsx)
- `SimplePieChart` – wykres kołowy rysowany na `Canvas`
- `colorFromHex(...)` – parsuje kolor zapisany jako string (#RRGGBB) na obiekt `Color`

---

### presentation/budget/Budget.kt
**Co to jest:** Ekran planowania budżetu miesięcznego.

**Co robi:**
- `BudgetViewModel` – łączy kategorie z planami budżetu, oblicza ile z planu już wydano
- `BudgetScreen` – lista kategorii wydatkowych z polem do wpisania planowanej kwoty, pokazuje ile zostało do planu, opcja skopiowania planu z poprzedniego miesiąca
- `BudgetRowCard` – jeden wiersz (kategoria + input + pasek postępu)

---

### presentation/savings/Savings.kt
**Co to jest:** Ekran skarbonka / oszczędności.

**Co robi:**
- `SavingsViewModel` – CRUD na skarbonkach
- `SavingsScreen` – lista skarbonka z aktualną kwotą i celem, możliwość dodania/edycji/usunięcia
- `JarCard` – jeden kafelek skarbonki z paskiem postępu
- `JarDialog` – dialog do tworzenia/edycji skarbonki

---

### presentation/settings/Settings.kt
**Co to jest:** Ekran ustawień.

**Co robi:**
- `SettingsViewModel` – CRUD na kategoriach
- `SettingsScreen` – przełącznik ciemnego motywu, lista kategorii wydatkowych i przychodowych z możliwością dodania/edycji/usunięcia (domyślne kategorie można tylko edytować, nie usunąć)
- `CategoryRow` – jeden wiersz kategorii
- `CategoryDialog` – dialog do tworzenia/edycji kategorii

---

### ui/theme/Theme.kt
**Co to jest:** Definicja kolorów i typografii całej aplikacji.

**Co robi:** Definiuje dwa schematy kolorów (jasny i ciemny) i podaje je do `MaterialTheme`. Każdy komponent Material 3 (Button, Card, NavigationBar) automatycznie bierze kolory z tego motywu – nie trzeba ręcznie podawać kolorów w każdym miejscu.

---

## Przepływ danych – jak to działa od kliknięcia do ekranu

Przykład: użytkownik dodaje wydatek 50 zł.

```
1. Użytkownik klika "Zapisz" w AddEditTransactionScreen
2. Screen wywołuje viewModel.save()
3. ViewModel wywołuje transactionRepo.insertTransaction(transaction)
4. Repo konwertuje model → entity i wywołuje dao.insertTransaction(entity)
5. Room zapisuje wiersz w SQLite
6. Room automatycznie emituje nową wartość w Flow<List<TransactionEntity>>
7. DashboardViewModel słucha tego Flow przez combine(...)
8. combine() oblicza nowy DashboardState (nowa suma wydatków)
9. _state.update { ... } emituje nowy stan
10. DashboardScreen robi collectAsState() i Compose przerysowuje ekran
```

Użytkownik wraca na Dashboard i widzi zaktualizowane liczby – bez ręcznego odświeżania.

---

---

# 20 pytań które może zadać prowadzący + odpowiedzi

---

**1. Co to jest Clean Architecture i dlaczego jej używasz?**

Clean Architecture to podział kodu na warstwy o ściśle określonych odpowiedzialnościach: UI, logika biznesowa, dostęp do danych. Używam jej bo każda warstwa można zmieniać niezależnie – np. można podmienić Room na inną bazę danych bez dotykania ani jednej linii kodu w ekranach. Poza tym logikę domenową można testować bez uruchamiania Androida.

---

**2. Co to jest MVVM i jak jest zaimplementowany w tym projekcie?**

MVVM to wzorzec Model–View–ViewModel. View (ekran Compose) wyświetla dane i przekazuje zdarzenia do ViewModel. ViewModel przetwarza dane i wystawia stan przez `StateFlow`. Model to dane z repozytorium. W projekcie każdy ekran ma swojego ViewModela – np. `DashboardViewModel` zbiera dane z 3 repozytoriów i oblicza `DashboardState`, który `DashboardScreen` obserwuje przez `collectAsState()`.

---

**3. Co to jest Room i jak działa?**

Room to biblioteka ORM (Object-Relational Mapping) od Google, będąca nakładką na SQLite. Piszesz klasy z adnotacjami (`@Entity`, `@Dao`, `@Database`) i Room w czasie kompilacji generuje cały kod obsługi bazy danych. W projekcie mamy 4 encje (tabele) i 4 DAO z metodami do zapytań.

---

**4. Co to jest KAPT i po co jest potrzebny?**

KAPT (Kotlin Annotation Processing Tool) to procesor adnotacji – narzędzie, które podczas budowania projektu czyta adnotacje (`@Entity`, `@Dao`) i **generuje kod źródłowy** implementujący te interfejsy. Bez KAPT Room nie wiedziałby jak zaimplementować DAO. To odpowiednik refleksji, ale wykonywany w czasie kompilacji, nie w czasie wykonania – dlatego jest szybszy i bezpieczniejszy.

---

**5. Co to jest Flow i dlaczego jest używany zamiast LiveData?**

`Flow` to strumień danych z biblioteki Kotlin Coroutines. Emituje kolejne wartości w czasie – Room używa go żeby automatycznie powiadamiać o zmianach w bazie. Wybrałem Flow zamiast LiveData bo: (1) Flow jest częścią czystego Kotlina, nie Androida – można testować bez instrumentacji, (2) Flow ma bogatszy zestaw operatorów (`combine`, `flatMapLatest`, `map`), (3) naturalnie integruje się z coroutines.

---

**6. Co robi operator `combine` i gdzie go używasz?**

`combine(flow1, flow2, flow3) { a, b, c -> ... }` czeka aż każdy z wejściowych Flow wyemituje co najmniej jedną wartość, potem za każdym razem gdy którykolwiek się zmieni – wywołuje blok transformacji z najnowszymi wartościami wszystkich. W `DashboardViewModel` kombinuję transakcje, kategorie i plany budżetu: gdy użytkownik doda nową transakcję, Room emituje nową listę transakcji, `combine` wywołuje blok, obliczam nowy `DashboardState` i Dashboard się aktualizuje.

---

**7. Co to jest StateFlow i jak różni się od zwykłego Flow?**

`StateFlow` to specjalny Flow który zawsze ma wartość (nie może być pusty), przechowuje ostatnią wyemitowaną wartość (hot stream), i każdy nowy kolektor od razu dostaje tę wartość. W przeciwieństwie do zwykłego `Flow` (cold stream), który wykonuje się od nowa dla każdego kolektora. W ViewModelach trzymam `MutableStateFlow<UiState>` i wystawiam go jako `StateFlow` – ekran dostaje zawsze aktualny stan, nawet jeśli zasubskrybował po fakcie.

---

**8. Co to jest Jetpack Compose i czym różni się od XML?**

Jetpack Compose to deklaratywne UI. Zamiast opisywać widoki w XML i ręcznie aktualizować je w kodzie (imperatywne), piszesz funkcje Kotlin z adnotacją `@Composable` które opisują jak UI ma wyglądać dla danego stanu. Gdy stan się zmienia, Compose automatycznie przerysowuje tylko te elementy które zmieniły swój wygląd (rekomposition). Jest to podobne do React.js w świecie web.

---

**9. Co to jest rekomposition?**

Rekomposition to mechanizm w Compose gdzie funkcja kompozycyjna jest wywoływana ponownie gdy zmienią się dane od których zależy. Compose jest na tyle inteligentny, że przerysowuje tylko te fragmenty UI które naprawdę muszą się zmienić – nie cały ekran. Żeby rekomposition działała, stan musi być przechowywany jako `State<T>` (np. przez `remember { mutableStateOf(...) }` lub `collectAsState()`).

---

**10. Po co `remember` i `mutableStateOf`?**

Bez `remember` stan byłby resetowany przy każdym wywołaniu funkcji kompozycyjnej (każdej rekomposition). `remember` mówi Compose żeby zapamiętał wartość między kolejnymi wywołaniami. `mutableStateOf` tworzy obserwowalny stan – zmiana jego wartości wyzwala rekomposition komponentów które go czytają. Przykład: `var showDialog by remember { mutableStateOf(false) }` – dialog jest zamknięty, po kliknięciu przycisku `showDialog = true`, Compose to widzi i przerysowuje ekran z otwartym dialogiem.

---

**11. Co to jest ViewModel i dlaczego przeżywa rotację ekranu?**

ViewModel to klasa przeznaczona do przechowywania i przetwarzania danych ekranu. Android trzyma go w specjalnym miejscu w pamięci powiązanym z "cyklem życia ekranu", nie Activity. Gdy obrócisz telefon, Activity jest niszczona i tworzona na nowo, ale ViewModel przeżywa – dzięki temu nie tracimy danych i nie robimy ponownych zapytań do bazy.

---

**12. Jak działa Dependency Injection w projekcie (bez Hilt)?**

Zamiast Hilt używam ręcznego DI przez `AppContainer`. `BudgetApp` (Application class) tworzy `AppContainer` który buduje bazę danych i repozytoria. Każdy ekran dostaje dostęp do kontenera przez `LocalContext.current.applicationContext as BudgetApp` i wywołuje `app.container`. ViewModel jest tworzony przez `ViewModelProvider.Factory` który dostaje repozytorium z kontenera. To prostsze niż Hilt, ale działa tak samo koncepcyjnie.

---

**13. Co to jest FileProvider i dlaczego jest potrzebny do eksportu Excel?**

Od Android 7.0 aplikacje nie mogą udostępniać pliku bezpośrednio przez ścieżkę w systemie plików – byłoby to zagrożenie bezpieczeństwa. FileProvider generuje tymczasowe `content://` URI z ograniczonym uprawnieniem do odczytu. Aplikacja docelowa (np. Gmail, Sheets) dostaje uprawnienie tylko do tego konkretnego pliku i tylko na czas aktywności `Intent.ACTION_SEND`. Bez FileProvider dostalibyśmy `FileUriExposedException`.

---

**14. Dlaczego baza danych jest singletonem?**

SQLite nie obsługuje wielu równoczesnych połączeń do zapisu bez ryzyka uszkodzenia bazy. Room wymaga żeby `RoomDatabase` był jeden na cały proces – tworzymy go przez `Room.databaseBuilder(...).build()` opakowany w `companion object` z `@Volatile`. Gdyby było wiele instancji, mogłyby powstać konflikty przy równoczesnym zapisie z różnych coroutines.

---

**15. Jak działa nawigacja w aplikacji?**

Używam Navigation Compose. `NavHost` zawiera graf wszystkich ekranów. Każdy ekran ma swój "adres" (route) zdefiniowany w sealed class `Screen`. Przejście do ekranu to `navController.navigate(Screen.AddEditTransaction.createRoute(id))`. Ekrany z dolnego paska (`popUpTo` + `launchSingleTop`) nie są duplikowane na stosie nawigacyjnym – kliknięcie Dashboardu gdy już jesteśmy na Dashboardzie nie dodaje kolejnej kopii.

---

**16. Co to jest suspend function i po co jest w projekcie?**

`suspend` to słowo kluczowe Kotlina oznaczające funkcję która może być wstrzymana bez blokowania wątku. Room wymaga `suspend` dla operacji zapisu (`insertTransaction`, `deleteTransaction`) – dzięki temu nie blokujemy głównego wątku UI (co spowodowałoby crash lub zawieszenie aplikacji). Funkcje suspend można wywoływać tylko z coroutine lub innej funkcji suspend – w ViewModelach używamy `viewModelScope.launch { ... }`.

---

**17. Co to jest `viewModelScope`?**

To coroutine scope powiązany z cyklem życia ViewModel. Gdy ViewModel jest niszczony (np. użytkownik wychodzi z ekranu), `viewModelScope` automatycznie anuluje wszystkie uruchomione w nim coroutines – nie musimy ręcznie zarządzać anulowaniem i nie ma wycieków pamięci.

---

**18. Dlaczego enum `TransactionType` jest zapisywany w bazie jako String?**

Room nie obsługuje natywnie enumów Kotlina. Można użyć `@TypeConverter` albo zapisać `name` enuma jako tekst (np. `"EXPENSE"`). Wybrałem prostsze podejście: w Entity kolumna `type` to `String`, przy odczycie robimy `TransactionType.valueOf(type)`. Alternatywą byłby `TypeConverter` zdefiniowany w bazie danych.

---

**19. Co to jest `flatMapLatest` i gdzie go używasz?**

`flatMapLatest` to operator Flow, który dla każdej nowej wartości ze strumienia wejściowego uruchamia nowy strumień wewnętrzny i **anuluje poprzedni**. W `TransactionListViewModel`:
```kotlin
_filter.flatMapLatest { f -> transactionRepo.getTransactionsByMonth(f.month) }
```
Gdy użytkownik zmieni miesiąc (`_filter` emituje nowy filtr), poprzednia obserwacja bazy dla starego miesiąca jest anulowana i zaczyna się nowa dla nowego miesiąca. Bez `flatMapLatest` moglibyśmy dostać dane z wielu miesięcy naraz.

---

**20. Jak działa wykres kołowy i co to jest Canvas?**

`Canvas` to kompozycja Compose dająca dostęp do API rysowania (DrawScope). W `SimplePieChart` iteruję po segmentach i dla każdego rysuję wycinek koła (`drawArc`) o kącie proporcjonalnym do wartości (`wartość / suma * 360°`). Zaczynamy od -90° (góra) i dokładamy kolejne segmenty. Jest to rysowanie niskopoziomowe – bezpośrednio na "płótnie" piksele po pikselu, bez żadnej biblioteki do wykresów.
