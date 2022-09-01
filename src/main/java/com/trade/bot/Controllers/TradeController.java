package com.trade.bot.Controllers;

import com.trade.bot.Services.TradeService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.Closeable;
import java.io.IOException;

@AllArgsConstructor
@NoArgsConstructor
@RestController
@RequestMapping(value = "/trade")
public class TradeController {

    @Autowired
    TradeService tradeService;

    @GetMapping(value = "/listen")
    public void listenForCurrentTickerPrice(String pairName) throws IOException {
     //   tradeService.getHistoricalData(pairName);
    }

    /*
    @GetMapping(value = "/get")
    public String getPrice(String ticker){
        return tradeService.getPrice(ticker);
    }

     */
}
