# BinanceTradingApp

simple application for trading on Binance using Java library for interacting with the Binance API, TA4j, jFreeChart.

Most things are hardcoded for now

Features:
* application opens websockets at start to listen for updates foreach symbol,
* user can change predefined condition in runtime for listener to print symbols with certain indicator value(cross EMA 9,26 for now only),
* chart printer to visualize price,
* trading strategy back-testing,
* trading strategy live-trading,
* possible time frames from 1 min to 1 week

http://localhost:8080/swagger-ui/index.html#

