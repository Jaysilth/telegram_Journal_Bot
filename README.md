# Trading Journal Bot 🤖📊

[![Java 21](https://img.shields.io/badge/java-21-blue.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/spring-boot-4.0.4-green.svg)](https://spring.io/projects/spring-boot)
[![Railway](https://img.shields.io/badge/deploy-railway-blue.svg)](https://railway.app)

Telegram bot for traders to log trades, generate performance reports, and get behavioral insights.

## 🚀 Features

- **📝 Trade Logging**: Parse trades with `SYMBOL BUY/SELL | Entry: 1.2345 | SL: 1.2300 | TP: 1.2400`
- **📈 Reports**: `/report` - Win rate, RR, session performance
- **🧠 Psychology**: `/psychology` - Hesitation analysis (missed trades)
- **📊 Dashboard**: `/dashboard` - Key metrics
- **👨‍🏫 Coach**: `/coach` - Execution feedback
- **PostgreSQL** + JPA persistence

## 🛠 Quick Start (Local)

```bash
# Clone
git clone https://github.com/Jaysilth/telegram_Journal_Bot
cd telegram_Journal_Bot

# Copy config
cp src/main/resources/application-example.properties src/main/resources/application.properties
# Edit: telegram.bot.token, telegram.chat.id, spring.datasource.*

# Run
./mvnw spring-boot:run
```

Bot listens on `http://localhost:8080/webhook`

## 🐳 Docker

```bash
docker build -t journalbot .
docker run -p 8080:8080 journalbot
```

## ☁️ Railway Deployment

1. `railway login`
2. `railway link`
3. `git push`

Dockerfile auto-builds with Java 21.

## ⚙️ Configuration

**application.properties:**
```properties
# Telegram
telegram.bot.token=YOUR_BOT_TOKEN
telegram.chat.id=YOUR_CHAT_ID

# Database (PostgreSQL)
spring.datasource.url=jdbc:postgresql://host:port/db
spring.datasource.username=user
spring.datasource.password=pass
spring.jpa.hibernate.ddl-auto=update
```

## 📱 Commands

```
/start - Bot active
/report - Performance report
/dashboard - Metrics overview
/psychology - Behavioral analysis
/coach - Execution feedback
/missed - Hesitation count
```

## 🏗 Tech Stack

- **Spring Boot 4.0.4** (WebMVC, Data JPA)
- **Java 21**
- **PostgreSQL** (HikariCP)
- **Lombok**
- **Maven** (wrapper)

## 📚 Trade Format

```
AAPL BUY
Entry: 150.25
SL: 148.50
TP: 154.00
Session: London
Strategy: Breakout
Outcome: TP
```

## 🤝 Contributing

1. Fork & clone
2. `git checkout -b feature/xyz`
3. `git commit -m "feat: description"`
4. `git push origin feature/xyz`
5. Open PR

## 🚀 Deployed

[Live Bot](https://railway.app/project/...) (after merge)

---

⭐ **Star on GitHub if useful!**

