<div align="center">

# 💰 ExpenseWise

### Smart Personal Finance Manager built with Java Swing

A modern desktop expense & income tracker designed to help users record transactions, monitor cash flow, manage budgets, analyze spending, and export financial data — all from a clean, responsive desktop interface.

<p>
  <img src="https://img.shields.io/badge/Java-8%2B-orange?style=for-the-badge&logo=openjdk" alt="Java 8+">
  <img src="https://img.shields.io/badge/GUI-Java%20Swing-blue?style=for-the-badge" alt="Java Swing">
  <img src="https://img.shields.io/badge/Storage-Local%20Files-success?style=for-the-badge" alt="Local Storage">
  <img src="https://img.shields.io/badge/Status-Active-6f42c1?style=for-the-badge" alt="Active">
</p>

<p>
  <a href="#-features">Features</a> •
  <a href="#-screenshots">Screenshots</a> •
  <a href="#-getting-started">Getting Started</a> •
  <a href="#-project-structure">Project Structure</a> •
  <a href="#-export-options">Export</a>
</p>

</div>

---

## 📌 About the Project

**ExpenseWise** is a Java-based personal finance management application with a polished dashboard-style UI. It supports multiple user profiles, transaction management, recurring expenses, monthly budgets, reports/export, profile customization, multiple currencies, and multiple interface languages.

The application is designed as a lightweight **offline-first desktop finance tool**, storing its data locally on the user's computer rather than requiring an online database.

---

## ✨ Features

### 📊 Dashboard
- Total balance overview
- Total income tracking
- Total expense tracking
- Current-month summary
- Net financial position
- Cash-flow visualization
- Spending-by-category visualization
- Recent transaction overview
- Monthly budget progress
- Quick actions for adding income/expenses and exporting data

### 💳 Transaction Management
- Add income and expenses
- Transaction date, description, category and amount
- Expense categories such as:
  - Food & Dining
  - Travel
  - Shopping
  - Bills & Utilities
  - Entertainment
  - Health
  - Education
  - Others
- Mark transactions as recurring
- Edit existing transactions
- Delete transactions
- Undo recent deletion
- Search and filter transactions
- Filter by date range, month, category and transaction type

### 🔁 Recurring Expenses
- Create recurring monthly transactions
- Automatically materialize recurring transactions when required
- Edit recurring transactions
- Delete recurring transactions

### 💰 Budget Management
- Set a monthly spending limit
- Track spending against the budget
- Visual budget progress indicator
- Reset budget when required

### 📈 Reports & Export
Export transaction data in multiple formats:

- 📄 PDF
- 📊 Excel `.xlsx`
- 📋 CSV

Exported files are generated locally through the application.

### 🎨 Themes
ExpenseWise includes three appearance modes:

- ☀️ Light
- 🌙 Dark
- 🌈 Gradient

The selected theme is saved in the application's preferences.

### 🌍 Multi-language UI
The interface includes translations for:

- English
- Hindi
- Spanish
- French
- German
- Japanese

### 💱 Currency Support
Supported currency symbols include:

- ₹ Indian Rupee
- $ US Dollar
- € Euro
- £ British Pound
- ¥ Japanese Yen

### 👤 User Profiles
- Create user profiles
- Login/logout
- Remember user option
- Email and phone profile information
- Profile photo support
- Edit profile
- Password change/reset flow
- Account deletion option

### 📱 Responsive Desktop Layout
The interface adapts its sidebar/navigation for smaller desktop window sizes and provides a collapsible navigation experience.

---

## 🖥️ Screenshots

### 🔐 Login Screen

<p align="center">
  <img src="docs/screenshots/login.png" alt="ExpenseWise Login Screen" width="850">
</p>

### 📊 Dashboard

<p align="center">
  <img src="docs/screenshots/dashboard.png" alt="ExpenseWise Dashboard" width="1000">
</p>

> **Tip:** Add more screenshots to `docs/screenshots/` as the project grows, for example Transactions, Reports, Settings and Dark/Gradient themes.

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| **Java 8+** | Core programming language |
| **Java Swing** | Desktop graphical user interface |
| **Java AWT** | UI drawing, events and responsive behavior |
| **Java NIO** | Local file-based data persistence |
| **Java ZIP APIs** | XLSX generation and archive handling |
| **Java ImageIO** | Profile image handling |
| **Java Security APIs** | Password hashing |

### Dependencies

The project is intentionally lightweight and does **not require a third-party dependency manager** for the core application. It uses Java's standard libraries.

---

## 🚀 Getting Started

### Prerequisites

Install **Java 8 or newer** and make sure Java is available from the terminal.

Check your installation:

```bash
java -version
javac -version
```

<<<<<<< HEAD
### ▶️ Run on Windows

The project includes a `run.bat` launcher.

**Option 1 — Double-click:**

```text
run.bat
```

**Option 2 — Terminal:**

```bash
java -cp . ExpenseWise
```

### 🧱 Compile manually

If the `.class` files are not present, compile the source first:

```bash
javac ExpenseWise.java
```

Then run:

```bash
=======
### Run with Windows batch file

```bat
run.bat
```

### Or compile manually

```bash
javac ExpenseWise.java
>>>>>>> 3eb4aa2 (Add professional README with screenshots)
java ExpenseWise
```

---

<<<<<<< HEAD
## 📁 Project Structure

```text
Expense-Wise/
│
├── ExpenseWise.java       # Main application source
├── run.bat                # Windows launcher
├── README.md              # Project documentation
├── .gitignore             # Git ignored files
│
└── docs/
    └── screenshots/
        ├── login.png
        └── dashboard.png
```

> Compiled `.class` files, crash logs and ZIP archives should remain ignored by Git through `.gitignore`.

---

## 💾 Local Data Storage

ExpenseWise stores application data locally under the user's home directory:

```text
~/.expensewise/
```

The application maintains local files for transaction data, users, preferences, budgets, profile details and profile photos.

This means the application can work without an online backend or cloud database.

---

## 🔐 Privacy & Security Notes

- Financial data is stored locally by the application.
- Passwords are processed using a hashing mechanism before being stored.
- No cloud database is required for normal application use.
- Avoid committing generated personal data or local application files to GitHub.
- Keep `.gitignore` enabled to prevent compiled files, logs and archives from being uploaded.

> **Production note:** For a real-world multi-user/cloud deployment, password storage should use a modern password-specific hashing algorithm such as Argon2id, bcrypt or scrypt, together with proper salting, rate limiting and secure secret management.

---

## 📤 Export Options

ExpenseWise provides local export functionality for the currently selected transaction data:

```text
PDF   → Printable/report-friendly document
XLSX  → Spreadsheet-compatible workbook
CSV   → Lightweight tabular data
=======
# 📁 Project Structure

```text
Expense Wise/
│
├── ExpenseWise.java
├── run.bat
├── .gitignore
├── README.md
│
└── docs/
    └── screenshots/
        ├── 01-login.png
        ├── 02-dashboard-light.png
        ├── 03-dashboard-dark.png
        ├── 04-add-transaction.png
        ├── 05-transactions.png
        ├── 06-recurring.png
        ├── 07-reports.png
        ├── 08-settings.png
        ├── 09-calendar.png
        └── 10-edit-profile.png
>>>>>>> 3eb4aa2 (Add professional README with screenshots)
```

---

<<<<<<< HEAD
## 🎯 Use Cases

ExpenseWise can be useful for:

- Personal expense tracking
- Monthly budget management
- Student finance tracking
- Household spending records
- Small personal financial reports
- Learning Java Swing and desktop application development

---

## 🔮 Future Improvements

Possible next upgrades:

- Cloud backup and synchronization
- PostgreSQL/MySQL database support
- Mobile companion app
- Advanced analytics and charts
- Custom categories
- Receipt image attachments
- Automatic backup/restore
- PIN/biometric desktop lock
- Scheduled recurring transactions
- More export templates
- Financial goals and savings tracking

---

## 🤝 Contributing

Contributions, ideas and improvements are welcome.
=======
# 💾 Data & Privacy

ExpenseWise is designed as an **offline-first desktop application**.

- Data is stored locally.
- No external database is required.
- Normal operation does not require internet access.
- User data is separated by profile.
- Passwords use SHA-256 hashing.

# 📤 Export Formats

| Format | Purpose |
|---|---|
| 📊 **Excel (.xlsx)** | Spreadsheet analysis |
| 📄 **PDF** | Printing and sharing |
| 🧾 **CSV** | Data processing and backup |

# 🌍 Customization

- ☀️ Light theme
- 🌙 Dark theme
- 🌈 Gradient theme
- 🌐 6 languages
- 💱 Multiple currency symbols
- 👤 Custom profile picture

# 🔮 Future Improvements

- Cloud backup
- Automatic database backup
- Mobile companion app
- Advanced financial analytics
- Custom report templates
- More currencies and languages
- Encrypted local storage
- Budget notifications
- Data import functionality

# 🤝 Contributing
>>>>>>> 3eb4aa2 (Add professional README with screenshots)

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Commit your changes
5. Push the branch
6. Open a Pull Request

<<<<<<< HEAD
Example:

```bash
git checkout -b feature/my-improvement
git add .
git commit -m "Add my improvement"
git push origin feature/my-improvement
```

---

## 📄 License

This project currently does not specify a license. If you plan to distribute or accept contributions publicly, add an appropriate license file such as `MIT License`.

---

<div align="center">

### 💰 ExpenseWise — Track. Analyze. Manage.

Built with ❤️ using Java Swing.

</div>
=======
# 📄 License

This project is currently intended for **personal / educational use**.

## 👨‍💻 Author

**Ankit Kumar**

Built with ❤️ using **Java Swing**.

---

<p align="center">
  ⭐ If you find ExpenseWise useful, consider giving the repository a star!
</p>
>>>>>>> 3eb4aa2 (Add professional README with screenshots)
