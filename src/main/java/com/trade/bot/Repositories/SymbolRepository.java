package com.trade.bot.Repositories;

import com.trade.bot.Models.Symbol;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface SymbolRepository extends CrudRepository<Symbol,String> {

    Optional<Symbol> findSymbolBySymbol(String symbol); // <?>

}
