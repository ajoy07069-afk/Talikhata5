package com.example.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromPartyType(value: PartyType): String = value.name

    @TypeConverter
    fun toPartyType(value: String): PartyType = PartyType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
