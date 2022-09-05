package com.trade.bot.Controllers;

import com.trade.bot.Services.TradeService;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;

@AllArgsConstructor
@NoArgsConstructor
@RestController
@RequestMapping
public class TradeController {

    @Autowired
    TradeService tradeService;

    @GetMapping(value = "/printChart")
    public void printChart(String pairName) throws IOException {
        tradeService.printChart(pairName);
    }

    @PostMapping(value = "/setConditionForScanner")
    public void addCondition(Boolean ifCrossEma926){
        tradeService.addCondition(ifCrossEma926);
    }
    @GetMapping(value = "/listen")
    public void listenForMarketData() throws IOException {
        tradeService.listenForMarketData();
    }

    @GetMapping(value = "/importSymbolsToTextFile")
    public void importSymbolsToFile() throws FileNotFoundException {
        tradeService.importSymbolsToFile();
    }
}
