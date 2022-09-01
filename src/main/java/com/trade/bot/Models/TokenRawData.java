package com.trade.bot.Models;

import lombok.*;
import org.ta4j.core.num.Num;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "float_data")
public class TokenRawData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    int id;
    @Column(name = "TokenSymbolPair")
    String TokenSymbolPair;
    @Column(name = "open_price")
    Float openPrice;
    @Column(name = "high_price")
    Float highPrice;
    @Column(name = "low_price")
    Float lowPrice;
    @Column(name = "close_price")
    Float closePrice;
    @Column(name = "volume")
    Float volume;
    @Column(name = "RSI")
    Double rsi;


}
