package com.trade.bot.Repositories;

import com.trade.bot.Models.CandlestickData;
import org.springframework.data.repository.CrudRepository;

public interface CandlestickDataRepository extends CrudRepository<CandlestickData,Integer> {
}
