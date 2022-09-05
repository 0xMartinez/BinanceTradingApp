package com.trade.bot.Models;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "float_data")
public class CandlestickData {

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
    @Column(name = "EMA9")
    Double EMA9;
    @Column(name = "EMA26")
    Double EMA26;



}
