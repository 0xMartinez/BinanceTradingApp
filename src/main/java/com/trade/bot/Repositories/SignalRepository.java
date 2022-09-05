package com.trade.bot.Repositories;

import com.trade.bot.Models.Signal;
import com.trade.bot.Models.Symbol;
import org.springframework.data.repository.CrudRepository;

public interface SignalRepository extends CrudRepository<Signal,String> {
}
