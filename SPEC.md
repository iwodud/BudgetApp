# BudgetApp — Specyfikacja aplikacji

## Cel aplikacji

Prosta, nowoczesna aplikacja Android do zarządzania osobistymi finansami. Działa w pełni lokalnie (offline-first). Umożliwia śledzenie wydatków, przychodów, planowania budżetu per kategoria i zarządzania oszczędnościami w formie skarbonek.

---

## Stack technologiczny

| Technologia | Zastosowanie |
|---|---|
| Kotlin | Język programowania |
| Jetpack Compose | UI |
| Navigation Compose | Nawigacja |
| Room Database | Lokalna baza danych |
| ViewModel + StateFlow | Zarządzanie stanem |
| MPAndroidChart lub Compose Charts | Wykresy |
| Apache POI | Export do pliku Excel (.xlsx) |
| Material 3 | Design system |

---

## Waluta

Wyłącznie PLN. Brak obsługi wielowalutowości.

---

## Typy transakcji

Aplikacja obsługuje 4 typy wpisów:

1. **Wydatek** — kwota, kategoria, data (opcjonalna), opis (opcjonalny)
2. **Przychód** — kwota, kategoria (np. Wynagrodzenie, Freelance, Inne przychody), data (opcjonalna), opis (opcjonalny)
3. **Zaoszczędzenie** — przeniesienie środków z portfela do wybranej skarbonki; zwiększa saldo skarbonki
4. **Pobranie z oszczędności** — przeniesienie środków ze skarbonki do portfela; zmniejsza saldo skarbonki

Typy 3 i 4 wymagają wyboru skarbonki. Oba są widoczne w historii transakcji.

---

## Funkcjonalności

### 1. Dashboard

Ekran główny z przeglądem finansów bieżącego miesiąca:

- Bilans miesiąca (przychody − wydatki)
- Suma wydatków miesiąca
- Suma przychodów miesiąca
- Pasek postępu łącznego budżetu (suma wydana vs suma planowana we wszystkich kategoriach)
- Ostatnie transakcje (5–10 wpisów)
- Skrót do dodawania transakcji (FAB)

### 2. Lista transakcji

- Wszystkie transakcje chronologicznie (wydatki, przychody, zaoszczędzenia, pobrania)
- Każdy wpis pokazuje: kwotę, typ, kategorię, datę (jeśli podana), opis (jeśli podany)
- Filtrowanie po:
  - kategorii
  - miesiącu
  - zakresie dat
  - typie transakcji
  - zakresie kwot (np. od 400 do 600 zł)
- Nowoczesny wygląd kart
- Obsługa przewijania

### 3. Dodawanie transakcji

Jeden ekran z wyborem typu (Wydatek / Przychód / Zaoszczędzenie / Pobranie z oszczędności):

**Wydatek / Przychód:**
- Kwota (wymagana)
- Kategoria — wybór kliknięciem z listy kafelków, możliwość dodania własnej
- Data — domyślnie dzisiaj, opcjonalnie zmień
- Opis (opcjonalny)

**Zaoszczędzenie / Pobranie z oszczędności:**
- Kwota (wymagana)
- Wybór skarbonki (wymagany)
- Data — domyślnie dzisiaj, opcjonalnie zmień
- Opis (opcjonalny)

Walidacja danych. Szybkie dodawanie (minimum kliknięć).

### 4. Edycja i usuwanie transakcji

- Edycja każdego pola istniejącej transakcji
- Usuwanie z dialogiem potwierdzenia

### 5. Planowanie budżetu miesięcznego

- Użytkownik ustawia planowaną kwotę wydatków **per kategoria** na dany miesiąc
- Kwoty są **edytowalne w dowolnym momencie** — to plan, nie sztywny limit
- Na ekranie widać: planowana kwota, wydana kwota, % realizacji per kategoria
- Wizualny pasek postępu per kategoria (zmienia kolor przy przekroczeniu planu — nie blokuje)
- **Historia planów:** każdy miesiąc przechowuje własny plan; przeglądanie historycznych planów i ich realizacji jest możliwe
- Możliwość **skopiowania planu z poprzedniego miesiąca** jako punkt startowy dla nowego miesiąca

### 6. Kategorie

**Domyślne kategorie wydatków:**
Jedzenie, Transport, Zakupy, Rozrywka, Rachunki, Zdrowie, Inne

**Domyślne kategorie przychodów:**
Wynagrodzenie, Freelance, Inne przychody

Użytkownik może:
- Dodawać własne kategorie (nazwa + kolor)
- Edytować **dowolną** kategorię — zarówno własną, jak i domyślną
- Usuwać własne kategorie (z potwierdzeniem; transakcje zachowują nazwę kategorii)
- Domyślnych kategorii nie można usunąć, ale można zmienić ich nazwę i kolor

### 7. Statystyki i wykresy

- Wykres kołowy — podział wydatków na kategorie (wybrany miesiąc)
- Zestawienie tekstowe: suma wydatków, największa kategoria, średni wydatek, suma przychodów, bilans

### 8. Oszczędności

- Wiele skarbonek — każda ma: nazwę, opcjonalny cel kwotowy, aktualną sumę
- Dodawanie / edycja / usuwanie skarbonek (z potwierdzeniem usunięcia)
- Saldo skarbonki zmienia się przez transakcje typu Zaoszczędzenie / Pobranie z oszczędności
- Widok wszystkich skarbonek z paskiem postępu (jeśli ustawiony cel)
- Łączna suma wszystkich oszczędności

### 9. Export do Excela

Trzy niezależne eksporty:

**Export 1 — Historia transakcji (miesięczna):**
Wybór miesiąca. Zawiera tylko wydatki, posortowane chronologicznie.
| Data | Kategoria | Opis | Kwota |
Jeśli brak daty lub opisu — pole puste.

**Export 2 — Podsumowanie kategorii (miesięczne):**
Wybór miesiąca. Wydatki zagregowane per kategoria.
| Kategoria | Suma wydatków |

**Export 3 — Podsumowanie roczne per kategoria:**
Wybór roku. Zestawienie wydatków per kategoria z podziałem na miesiące.
| Kategoria | Styczeń | Luty | ... | Grudzień | Razem |

Format: `.xlsx`. Plik udostępniany przez system share sheet (możliwość zapisu, wysyłki itp.).

### 10. Ustawienia

- Przełącznik Dark Mode / Light Mode (domyślnie Light)
- Zarządzanie kategoriami (lista + edycja + dodawanie)
- Informacja o aplikacji (wersja)

---

## Dane przykładowe

Aplikacja startuje z predefiniowanymi przykładowymi danymi, aby użytkownik od razu zobaczył jak wygląda wypełniona aplikacja:

- Kilkanaście transakcji (wydatki i przychody) z ostatnich 2 miesięcy
- Przykładowe plany budżetowe dla bieżącego miesiąca
- Dwie przykładowe skarbonki (np. „Wakacje", „Fundusz awaryjny") z saldem
- Kilka przykładowych transferów do skarbonek

---

## UI/UX

- **Design system:** Material 3
- **Motyw:** Light mode domyślnie, Dark mode jako opcja w ustawieniach
- **Podejście do UI:** czyste, funkcjonalne, bez nadmiernych ozdób — prostota na pierwszym miejscu
- **Kolorystyka:** neutralna, profesjonalna z wyraźnym akcentem kolorystycznym
- Empty states (gdy brak transakcji, brak skarbonek)
- Loading states
- Responsywność na różne rozmiary ekranów

---

## Nawigacja

```
Bottom Navigation Bar:
├── Dashboard
├── Transakcje (Lista)
├── [FAB: Dodaj transakcję]
├── Statystyki
└── Ustawienia

Osobne ekrany (push):
├── Dodaj/Edytuj transakcję
├── Planowanie budżetu
└── Oszczędności (lista skarbonek + szczegół skarbonki)
```

---

## Architektura — Clean Architecture

```
app/
├── data/
│   ├── local/
│   │   ├── dao/           (TransactionDao, CategoryDao, BudgetPlanDao, SavingsDao)
│   │   ├── entity/        (TransactionEntity, CategoryEntity, BudgetPlanEntity, SavingsJarEntity)
│   │   └── AppDatabase.kt
│   └── repository/        (implementacje repozytoriów)
├── domain/
│   ├── model/             (Transaction, Category, BudgetPlan, SavingsJar)
│   ├── repository/        (interfejsy repozytoriów)
│   └── usecase/           (GetTransactionsUseCase, AddTransactionUseCase, itp.)
└── presentation/
    ├── dashboard/
    ├── transactions/
    ├── statistics/
    ├── budget/
    ├── savings/
    ├── settings/
    └── common/            (reużywalne komponenty Compose)
```

### Wzorce:
- **MVVM** — ViewModel + StateFlow + Sealed classes dla UI state
- **Repository pattern** — odizolowanie źródła danych
- **Use cases** — logika biznesowa poza ViewModelem
- Coroutines + Flow do operacji asynchronicznych

---

## Baza danych — Room

### Encje:

**TransactionEntity**
```
id, type (EXPENSE / INCOME / SAVE_TO_JAR / WITHDRAW_FROM_JAR),
amount, categoryId?, description?, date?, savingsJarId?, createdAt
```
- `categoryId` wymagane dla EXPENSE i INCOME; null dla SAVE_TO_JAR i WITHDRAW_FROM_JAR
- `savingsJarId` wymagane dla SAVE_TO_JAR i WITHDRAW_FROM_JAR; null dla pozostałych

**CategoryEntity**
```
id, name, type (EXPENSE / INCOME), isDefault, colorHex
```

**BudgetPlanEntity**
```
id, categoryId, month (YYYY-MM), plannedAmount
```

**SavingsJarEntity**
```
id, name, currentAmount, goalAmount?, createdAt
```

**SettingsEntity**
```
key, value
```

### Migracje:
Przygotowane od wersji 1 z możliwością dodania kolejnych migracji.

---

## Jakość kodu

- Zgodność z zasadami SOLID
- Sealed classes dla stanów UI (`Loading`, `Success`, `Error`, `Empty`)
- Reużywalne komponenty Compose
- Preview dla każdego composable
- Obsługa rotacji ekranu (ViewModel survives config changes)
- Error handling z komunikatami dla użytkownika
- Kod komentowany tam, gdzie logika jest nieoczywista

---

## Co NIE jest w zakresie

- Synchronizacja z chmurą
- Wielowalutowość
- Powiadomienia push
- Widżety systemowe
- Logowanie / konta użytkowników
