package com.trade.bot.Models;


import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "Symbols")
public class Symbol {

    @Id
    @Column(name = "symbol")
    String symbol;
    @Column(name = "EMA9")
    Double EMA9;
    @Column(name = "EMA26")
    Double EMA26;
    @Column(name = "close_price")
    Float closePrice;
    @Column(name = "ema_diff")
    Double EMAdiff;
    @Column(name = "ema_diff_in_percent")
    Double EMAdiffInPercent;
}
