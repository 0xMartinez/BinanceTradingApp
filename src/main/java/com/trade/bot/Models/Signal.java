package com.trade.bot.Models;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "Signals")
public class Signal {

    @Id
    @Column(name = "symbol")
    String symbol;
    @Column(name = "EMA9")
    Double EMA9;
    @Column(name = "EMA26")
    Double EMA26;
    @Column(name = "buy_close_price_at_signal")
    Float buyClosePriceAtSignal;
    @Column(name = "sell_close_price_at_signal")
    Float sellClosePriceAtSignal;
    @Column(name = "buy_sell")
    String Operations;
}
