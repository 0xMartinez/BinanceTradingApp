package com.trade.bot.Services;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

import com.trade.bot.Models.TokenRawData;
import com.binance.api.client.domain.account.Trade;

import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.*;

import com.trade.bot.Repositories.TokenRawDataRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.AverageProfitableTradesCriterion;
import org.ta4j.core.analysis.criteria.RewardRiskRatioCriterion;
import org.ta4j.core.analysis.criteria.TotalProfitCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.trading.rules.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

@Slf4j
@Service
@AllArgsConstructor
@NoArgsConstructor
public class TradeService {

    @Autowired
    TokenRawDataRepository tokenRawDataRepository;

    public void binanceCoinScanner() throws FileNotFoundException {
        BinanceApiClientFactory factory_ = BinanceApiClientFactory.newInstance("","");
        BinanceApiRestClient client_ = factory_.newRestClient();
        List<String> symbols = new ArrayList<String>();

        PrintWriter outputStream = new PrintWriter(new FileOutputStream("symbols.txt"));

        int j =0;
        for(int i = 0; i < client_.getBookTickers().size(); i++) {
            if(client_.getBookTickers().get(i).getSymbol().endsWith("USDT")
                    && !(client_.getBookTickers().get(i).getSymbol().contains("UP"))
                    && !(client_.getBookTickers().get(i).getSymbol().contains("DOWN"))
                    && !(client_.getBookTickers().get(i).getSymbol().contains("BEAR"))
                    && !(client_.getBookTickers().get(i).getSymbol().contains("BULL"))
            ){
                symbols.add(client_.getBookTickers().get(i).getSymbol());
                System.out.println(j + "." +client_.getBookTickers().get(i).getSymbol());
                outputStream.println(client_.getBookTickers().get(i).getSymbol()+",");
                j++;
            }
        }
        outputStream.close();
    }

    public void readFromFile() throws IOException {

        BinanceApiClientFactory factory_ = BinanceApiClientFactory.newInstance("", "");
        BinanceApiRestClient client_ = factory_.newRestClient();
        List<String> symbols = new ArrayList<String>();
        Scanner scanner = new Scanner(new File("symbols.txt"));

        int i = 0;
        while (scanner.hasNextLine()) {

            String symbol = scanner.nextLine();
            symbols.add(symbol);
            System.out.println(symbols.get(i));
            BarSeries series = new BaseBarSeries(symbols.get(i));
            List<Candlestick> candlesticks = client_.getCandlestickBars(symbols.get(i), CandlestickInterval.ONE_MINUTE);
            for (Candlestick s : candlesticks) {
                TokenRawData tokenRawData = new TokenRawData();
                ClosePriceIndicator closePrice_ = new ClosePriceIndicator(series);
                RSIIndicator rsiIndicator = new RSIIndicator(closePrice_, 5);
                tokenRawData.setHighPrice(Float.parseFloat(s.getHigh()));
                tokenRawData.setOpenPrice(Float.parseFloat(s.getOpen()));
                tokenRawData.setLowPrice(Float.parseFloat(s.getLow()));
                tokenRawData.setClosePrice(Float.parseFloat(s.getClose()));
                tokenRawData.setVolume(Float.parseFloat(s.getVolume()));
                tokenRawData.setTokenSymbolPair(symbols.get(i));
                tokenRawData.setRsi((rsiIndicator.getValue((series.getEndIndex()))).doubleValue());

                tokenRawDataRepository.save(tokenRawData);

                ZonedDateTime zdt = Instant
                        .ofEpochMilli(s.getCloseTime())
                        .atZone(ZoneId.of("Europe/Warsaw"));

                series.addBar(zdt, tokenRawData.getOpenPrice(), tokenRawData.getHighPrice(), tokenRawData.getLowPrice(), tokenRawData.getClosePrice(), tokenRawData.getVolume());

            }

            ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
            TimeSeries chartTimeSeries = new TimeSeries("");

            TimeSeriesCollection dataset = new TimeSeriesCollection();

            for (int ii = 0; ii < series.getBarCount(); ii++) {

                Bar bar = series.getBar(ii);
                chartTimeSeries.addOrUpdate(new Minute(Date.from(bar.getEndTime().toInstant())), closePrice.getValue(ii).doubleValue());
            }


            dataset.addSeries(chartTimeSeries);
            JFreeChart chart = ChartFactory.createTimeSeriesChart(symbols.get(i), // title
                    "Date", // x-axis label
                    "Price", // y-axis label
                    dataset, // data
                    true, // create legend?
                    true, // generate tooltips?
                    false // generate URLs?
            );

            XYPlot plot = (XYPlot) chart.getPlot();
            DateAxis axis = (DateAxis) plot.getDomainAxis();
            axis.setDateFormatOverride(new SimpleDateFormat("h:mm a"));
            ChartPanel panel = new ChartPanel(chart);
            ChartUtils.saveChartAsJPEG(new File("charts/"+symbols.get(i)+".jpeg"), panel.getChart(), 3000, 2000);
            i++;

        }

    }
    public void SpotHistAndCurrentChart(String pairName) throws IOException {

        BinanceApiClientFactory factory_ = BinanceApiClientFactory.newInstance("","");
        BinanceApiRestClient client_ = factory_.newRestClient();

        BinanceApiWebSocketClient client = BinanceApiClientFactory.newInstance("","").newWebSocketClient();
        List<Candlestick> candlesticks = client_.getCandlestickBars("BTCUSDT", CandlestickInterval.ONE_MINUTE);

        BarSeries series = new BaseBarSeries("my_live_series");

        for (Candlestick s : candlesticks) {
            TokenRawData tokenRawData = new TokenRawData();
            ClosePriceIndicator closePrice_ = new ClosePriceIndicator(series);
            RSIIndicator rsiIndicator = new RSIIndicator(closePrice_, 5);
            tokenRawData.setHighPrice(Float.parseFloat(s.getHigh()));
            tokenRawData.setOpenPrice(Float.parseFloat(s.getOpen()));
            tokenRawData.setLowPrice(Float.parseFloat(s.getLow()));
            tokenRawData.setClosePrice(Float.parseFloat(s.getClose()));
            tokenRawData.setVolume(Float.parseFloat(s.getVolume()));
            tokenRawData.setTokenSymbolPair(pairName);
            tokenRawData.setRsi((rsiIndicator.getValue((series.getEndIndex()))).doubleValue());

            tokenRawDataRepository.save(tokenRawData);

            ZonedDateTime zdt = Instant
                    .ofEpochMilli(s.getCloseTime())
                    .atZone(ZoneId.of("Europe/Warsaw"));

            series.addBar(zdt, tokenRawData.getOpenPrice(), tokenRawData.getHighPrice(), tokenRawData.getLowPrice(), tokenRawData.getClosePrice(), tokenRawData.getVolume());
            if(series.getBarCount()==500){
                Rule crossedDownIndicatorRule = new CrossedDownIndicatorRule(rsiIndicator, 20);
                Rule crossedUpIndicatorRule = new CrossedUpIndicatorRule(rsiIndicator, 60);
                Strategy strategy = new BaseStrategy(crossedDownIndicatorRule, crossedUpIndicatorRule);

                BarSeriesManager manager = new BarSeriesManager(series);
                TradingRecord tradingRecord = manager.run(strategy);
                System.out.println("Number of trades for our strategy: " + tradingRecord.getTradeCount());
                AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
                System.out.println("Profitable trades ratio: " + profitTradesRatio.calculate(series, tradingRecord));
                AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
                System.out.println("Reward-risk ratio: " + rewardRiskRatio.calculate(series, tradingRecord));

                AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
                System.out.println("Our profit vs buy-and-hold profit: " + vsBuyAndHold.calculate(series, tradingRecord));
            }


        }
        log.info(series.getBarData() + "\n");
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        TimeSeries chartTimeSeries = new TimeSeries("");

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        for (int i = 0; i < series.getBarCount(); i++) {

            Bar bar = series.getBar(i);
            chartTimeSeries.addOrUpdate(new Minute(Date.from(bar.getEndTime().toInstant())), closePrice.getValue(i).doubleValue());
        }


        dataset.addSeries(chartTimeSeries);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(pairName, // title
                "Date", // x-axis label
                "Price", // y-axis label
                dataset, // data
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
        );

        XYPlot plot = (XYPlot) chart.getPlot();
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("h:mm a"));
        ChartPanel panel = new ChartPanel(chart);
        ChartUtils.saveChartAsJPEG(new File("filename.jpeg"), panel.getChart(), 3000, 2000);

        // Account account = client_.getAccount(50000L,ZonedDateTime.now().getLong());

        client.onCandlestickEvent("btcusdt", CandlestickInterval.ONE_MINUTE, response -> {
            // Save only if candle stick bar is final
            if (response.getBarFinal()) {
                TokenRawData tokenRawData = new TokenRawData();
                ClosePriceIndicator closePrice_ = new ClosePriceIndicator(series);
                RSIIndicator rsiIndicator = new RSIIndicator(closePrice_, 9);
                EMAIndicator emaIndicator9 = new EMAIndicator(closePrice_,9);
                EMAIndicator emaIndicator26 = new EMAIndicator(closePrice_,26);
                EMAIndicator emaIndicator3 = new EMAIndicator(closePrice_, 3);
                tokenRawData.setTokenSymbolPair(pairName);
                tokenRawData.setHighPrice(Float.parseFloat(response.getHigh()));
                tokenRawData.setOpenPrice(Float.parseFloat(response.getOpen()));
                tokenRawData.setLowPrice(Float.parseFloat(response.getLow()));
                tokenRawData.setClosePrice(Float.parseFloat(response.getClose()));
                tokenRawData.setVolume(Float.parseFloat(response.getVolume()));
                tokenRawDataRepository.save(tokenRawData);
                series.addBar(ZonedDateTime.now(), tokenRawData.getOpenPrice(), tokenRawData.getHighPrice(), tokenRawData.getLowPrice(), tokenRawData.getClosePrice(), tokenRawData.getVolume());
                tokenRawData.setRsi((rsiIndicator.getValue((series.getEndIndex()))).doubleValue());
                Rule crossedDownIndicatorRule = new CrossedDownIndicatorRule(emaIndicator3, emaIndicator9).or(new StopGainRule(closePrice_, 0.2)).or(new StopLossRule(closePrice_, 0.5));
                Rule crossedUpIndicatorRule = new CrossedUpIndicatorRule(emaIndicator9, emaIndicator26);
                System.out.println(emaIndicator9.getValue(series.getEndIndex()) + " ----- " + emaIndicator26.getValue(series.getEndIndex()));
                //if short crosses long then buy
                //if long crosses short then sell
                if(crossedUpIndicatorRule.isSatisfied(series.getEndIndex())) {
                    System.out.println("Buying");
                    if(Float.parseFloat(client_.getAccount(50000L, response.getCloseTime()).getAssetBalance("USDT").getFree())>100.0) {
                        NewOrderResponse newOrderResponse = client_.newOrder(marketBuy("BTCUSDT", "0.046").recvWindow(50000L));
                        // NewOrderResponse newOrderResponse = client_.newOrder(marketBuy("BTCUSDT", Float.toString((Float.parseFloat(client_.getAccount().getAssetBalance("USDT").getFree()) / Float.parseFloat(response.getClose())))).recvWindow(50000L));
                        System.out.println(newOrderResponse.getClientOrderId());
                    }
                }
                else if(crossedDownIndicatorRule.isSatisfied(series.getEndIndex())){
                    if(Float.parseFloat(client_.getAccount(50000L, response.getCloseTime()).getAssetBalance("BTC").getFree())>0.0){
                        System.out.println("Selling");
                        NewOrderResponse newOrderResponse = client_.newOrder(marketSell("BTCUSDT",client_.getAccount(50000L, response.getCloseTime()).getAssetBalance("BTC").getFree()).recvWindow(50000L));
                        List<Trade> fills = newOrderResponse.getFills();
                        System.out.println(newOrderResponse.getClientOrderId());
                    }
                }
                tokenRawDataRepository.save(tokenRawData);

            } else {
                log.info("kline is not final open_time={}", response.getClose());

            }
        });

    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) throws IOException {
        log.info("starting service");
        readFromFile();
        //binanceCoinScanner();
        //  SpotHistAndCurrentChart("btcusdt");
    }

}