package com.trade.bot.Services;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

import com.trade.bot.Models.Condition;
import com.trade.bot.Models.Signal;
import com.trade.bot.Models.Symbol;
import com.trade.bot.Models.CandlestickData;
import com.binance.api.client.domain.account.Trade;

import com.trade.bot.Repositories.ConditionRepository;
import com.trade.bot.Repositories.SignalRepository;
import com.trade.bot.Repositories.SymbolRepository;
import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.*;

import com.trade.bot.Repositories.CandlestickDataRepository;
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
import java.util.*;

import static com.binance.api.client.domain.account.NewOrder.marketBuy;
import static com.binance.api.client.domain.account.NewOrder.marketSell;

@Slf4j
@Service
@AllArgsConstructor
@NoArgsConstructor
public class TradeService {

    @Autowired
    CandlestickDataRepository candlestickDataRepository;

    @Autowired
    SymbolRepository symbolRepository;

    @Autowired
    SignalRepository signalRepository;

    @Autowired
    ConditionRepository conditionRepository;

    public void addCondition(Boolean ifCrossEma926){
        if(conditionRepository.count()<=0){
            Condition condition = new Condition(ifCrossEma926);
            conditionRepository.save(condition);
        }
    }

    public void printChart(String pairName) throws IOException {

        BinanceApiClientFactory factory_ = BinanceApiClientFactory.newInstance();
        BinanceApiRestClient client_ = factory_.newRestClient();
        List<Candlestick> candlesticks = client_.getCandlestickBars(pairName, CandlestickInterval.HOURLY);//for now hard coded 1 hour interval

        BarSeries series = new BaseBarSeries("pairName"+" series");

        for (Candlestick s : candlesticks) {
            ZonedDateTime zdt = Instant
                    .ofEpochMilli(s.getCloseTime())
                    .atZone(ZoneId.of("Europe/Warsaw"));

            series.addBar(zdt, Float.parseFloat(s.getOpen()), Float.parseFloat(s.getHigh()), Float.parseFloat(s.getLow()), Float.parseFloat(s.getClose()), Float.parseFloat(s.getVolume()));

        }
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        TimeSeries chartTimeSeries = new TimeSeries("");

        TimeSeriesCollection dataset = new TimeSeriesCollection();

        for (int i = 0; i < series.getBarCount(); i++) {

            Bar bar = series.getBar(i);
            chartTimeSeries.addOrUpdate(new Hour(Date.from(bar.getEndTime().toInstant())), closePrice.getValue(i).doubleValue());
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
        ChartUtils.saveChartAsJPEG(new File("charts/"+pairName+".jpeg"), panel.getChart(), 3000, 2000);
        }

    public void importSymbolsToFile() throws FileNotFoundException {
        BinanceApiClientFactory factory_ = BinanceApiClientFactory.newInstance();
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
                outputStream.println(client_.getBookTickers().get(i).getSymbol());
                j++;
            }
        }
        outputStream.close();
    }

    public void listenForMarketData() throws IOException {

        BinanceApiClientFactory factory_ = BinanceApiClientFactory.newInstance();
        BinanceApiWebSocketClient client = BinanceApiClientFactory.newInstance().newWebSocketClient();
        BinanceApiRestClient client_ = factory_.newRestClient();
        Scanner scanner = new Scanner(new File("symbols.txt"));

        int i = 0;
        while (scanner.hasNextLine()) {
            String symbolFromFile = scanner.nextLine();
            if ((symbolRepository.findSymbolBySymbol(symbolFromFile).isEmpty())) {
                Symbol symbol = new Symbol(symbolFromFile,0.0,0.0,0f,0.0,0.0);
                symbolRepository.save(symbol);
                System.out.println(i + "." + symbol.getSymbol());

                BarSeries series = new BaseBarSeries(symbol.getSymbol());
                List<Candlestick> candlesticks = client_.getCandlestickBars(symbol.getSymbol(), CandlestickInterval.HOURLY);
                for (Candlestick s : candlesticks) {
                    CandlestickData candlestickData = new CandlestickData();
                    ClosePriceIndicator closePrice_ = new ClosePriceIndicator(series);
                    RSIIndicator rsiIndicator = new RSIIndicator(closePrice_, 5);
                    EMAIndicator emaIndicator9 = new EMAIndicator(closePrice_,9);
                    EMAIndicator emaIndicator26 = new EMAIndicator(closePrice_,26);
                    candlestickData.setHighPrice(Float.parseFloat(s.getHigh()));
                    candlestickData.setOpenPrice(Float.parseFloat(s.getOpen()));
                    candlestickData.setLowPrice(Float.parseFloat(s.getLow()));
                    candlestickData.setClosePrice(Float.parseFloat(s.getClose()));
                    candlestickData.setVolume(Float.parseFloat(s.getVolume()));
                    candlestickData.setTokenSymbolPair(symbol.getSymbol());
                    candlestickData.setRsi((rsiIndicator.getValue((series.getEndIndex()))).doubleValue());
                    if(series.getEndIndex()>=9){
                        candlestickData.setEMA9(emaIndicator9.getValue(series.getEndIndex()).doubleValue());
                    }
                    if(series.getEndIndex()>=26){
                        candlestickData.setEMA9(emaIndicator26.getValue(series.getEndIndex()).doubleValue());

                    }
                    candlestickDataRepository.save(candlestickData);

                    ZonedDateTime zdt = Instant
                            .ofEpochMilli(s.getCloseTime())
                            .atZone(ZoneId.of("Europe/Warsaw"));

                    series.addBar(zdt, candlestickData.getOpenPrice(), candlestickData.getHighPrice(), candlestickData.getLowPrice(), candlestickData.getClosePrice(), candlestickData.getVolume());

                }
                i++;

                client.onCandlestickEvent(symbol.getSymbol().toLowerCase(), CandlestickInterval.HOURLY, response -> {
                    series.addPrice(response.getClose());
                    ClosePriceIndicator closePrice_ = new ClosePriceIndicator(series);
                    CandlestickData candlestickData = new CandlestickData();
                    RSIIndicator rsiIndicator = new RSIIndicator(closePrice_, 9);
                    EMAIndicator emaIndicator9 = new EMAIndicator(closePrice_,9);
                    EMAIndicator emaIndicator26 = new EMAIndicator(closePrice_,26);
                    EMAIndicator emaIndicator3 = new EMAIndicator(closePrice_, 3);

                    symbol.setClosePrice(Float.parseFloat(response.getClose()));
                    symbol.setEMA9(emaIndicator9.getValue(series.getEndIndex()).doubleValue());
                    symbol.setEMA26(emaIndicator26.getValue(series.getEndIndex()).doubleValue());
                    double EMAdiff = symbol.getEMA26() - symbol.getEMA9();
                    double previousEMAdiff = emaIndicator26.getValue(series.getEndIndex()-1).doubleValue() - emaIndicator9.getValue(series.getEndIndex()-1).doubleValue();
                    symbol.setEMAdiff(EMAdiff);

                    symbol.setEMAdiffInPercent((EMAdiff*100)/symbol.getClosePrice());
                    symbolRepository.save(symbol);

                    if(response.getBarFinal()){
                        candlestickData.setTokenSymbolPair(symbol.getSymbol());
                        candlestickData.setHighPrice(Float.parseFloat(response.getHigh()));
                        candlestickData.setOpenPrice(Float.parseFloat(response.getOpen()));
                        candlestickData.setLowPrice(Float.parseFloat(response.getLow()));
                        candlestickData.setClosePrice(Float.parseFloat(response.getClose()));
                        candlestickData.setVolume(Float.parseFloat(response.getVolume()));
                        candlestickData.setEMA9(emaIndicator9.getValue(series.getEndIndex()).doubleValue());
                        candlestickData.setEMA26(emaIndicator26.getValue(series.getEndIndex()).doubleValue());
                        candlestickDataRepository.save(candlestickData);
                        series.addBar(ZonedDateTime.now(), Float.parseFloat(response.getOpen()), Float.parseFloat(response.getHigh()), Float.parseFloat(response.getLow()), Float.parseFloat(response.getClose()), Float.parseFloat(response.getVolume()));
                    }
                    if(conditionRepository.count()>0){
                        conditionRepository.findAll().forEach(condition -> {
                            if(condition.getIfCrossEma926()){
                                if(previousEMAdiff>0.0000000 && EMAdiff<0.00000000){
                                    if(signalRepository.findById(symbol.getSymbol()).isEmpty()) {
                                        Signal signal = new Signal(symbol.getSymbol(),symbol.getEMA9(),symbol.getEMA26(),symbol.getClosePrice(),0.000f,"BUY");
                                        signalRepository.save(signal);
                                        System.out.println(symbol.getSymbol() + "-----" + "BUY");
                                    }
                                }
                            }
                        });
                    }
                });
            }
        }
    }

    public void SpotHistAndCurrentChart(String pairName) throws IOException {

        BinanceApiClientFactory factory_ = BinanceApiClientFactory.newInstance("","");
        BinanceApiRestClient client_ = factory_.newRestClient();

        BinanceApiWebSocketClient client = BinanceApiClientFactory.newInstance("","").newWebSocketClient();
        List<Candlestick> candlesticks = client_.getCandlestickBars("BTCUSDT", CandlestickInterval.ONE_MINUTE);

        BarSeries series = new BaseBarSeries("my_live_series");

        for (Candlestick s : candlesticks) {
            CandlestickData candlestickData = new CandlestickData();
            ClosePriceIndicator closePrice_ = new ClosePriceIndicator(series);
            RSIIndicator rsiIndicator = new RSIIndicator(closePrice_, 5);
            candlestickData.setHighPrice(Float.parseFloat(s.getHigh()));
            candlestickData.setOpenPrice(Float.parseFloat(s.getOpen()));
            candlestickData.setLowPrice(Float.parseFloat(s.getLow()));
            candlestickData.setClosePrice(Float.parseFloat(s.getClose()));
            candlestickData.setVolume(Float.parseFloat(s.getVolume()));
            candlestickData.setTokenSymbolPair(pairName);
            candlestickData.setRsi((rsiIndicator.getValue((series.getEndIndex()))).doubleValue());

            candlestickDataRepository.save(candlestickData);

            ZonedDateTime zdt = Instant
                    .ofEpochMilli(s.getCloseTime())
                    .atZone(ZoneId.of("Europe/Warsaw"));

            series.addBar(zdt, candlestickData.getOpenPrice(), candlestickData.getHighPrice(), candlestickData.getLowPrice(), candlestickData.getClosePrice(), candlestickData.getVolume());
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
                CandlestickData candlestickData = new CandlestickData();
                ClosePriceIndicator closePrice_ = new ClosePriceIndicator(series);
                RSIIndicator rsiIndicator = new RSIIndicator(closePrice_, 9);
                EMAIndicator emaIndicator9 = new EMAIndicator(closePrice_,9);
                EMAIndicator emaIndicator26 = new EMAIndicator(closePrice_,26);
                EMAIndicator emaIndicator3 = new EMAIndicator(closePrice_, 3);
                candlestickData.setTokenSymbolPair(pairName);
                candlestickData.setHighPrice(Float.parseFloat(response.getHigh()));
                candlestickData.setOpenPrice(Float.parseFloat(response.getOpen()));
                candlestickData.setLowPrice(Float.parseFloat(response.getLow()));
                candlestickData.setClosePrice(Float.parseFloat(response.getClose()));
                candlestickData.setVolume(Float.parseFloat(response.getVolume()));
                candlestickDataRepository.save(candlestickData);
                series.addBar(ZonedDateTime.now(), candlestickData.getOpenPrice(), candlestickData.getHighPrice(), candlestickData.getLowPrice(), candlestickData.getClosePrice(), candlestickData.getVolume());
                candlestickData.setRsi((rsiIndicator.getValue((series.getEndIndex()))).doubleValue());
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
                candlestickDataRepository.save(candlestickData);

            } else {
                log.info("kline is not final open_time={}", response.getClose());

            }
        });

    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) throws IOException {
        log.info("starting service");
        listenForMarketData();
    }


}