package com.trade.bot.Models;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "Conditions")
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "condition_id")
    int conditionId;
    @Column(name = "cross_EMA_9_26_strat")
    Boolean ifCrossEma926;

    public Condition(Boolean ifCrossEma926) {
        this.ifCrossEma926 = ifCrossEma926;
    }
}
