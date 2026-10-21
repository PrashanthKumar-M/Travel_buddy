package com.travelbuddy.app.domain

data class Trip(val id:String,val name:String,val source:String,val destination:String,val dates:String,val status:TripStatus,val members:List<TripMember>)
enum class TripStatus { PLANNING, LIVE, COMPLETED }
data class TripMember(val id:String,val name:String,val color:Long,val isSharingLocation:Boolean)
data class Expense(val id:String,val title:String,val amount:Double,val paidBy:String,val splitWith:Set<String>)
data class Balance(val memberId:String,val paid:Double,val owes:Double){ val net:Double get()=paid-owes }
data class Settlement(val from:String,val to:String,val amount:Double)

object ExpenseCalculator {
    fun balances(members:List<TripMember>,expenses:List<Expense>)=members.map{m->
        val paid=expenses.filter{it.paidBy==m.id}.sumOf{it.amount}
        val owes=expenses.filter{m.id in it.splitWith}.sumOf{it.amount/it.splitWith.size}
        Balance(m.id,paid,owes)
    }
    fun settlements(balances:List<Balance>):List<Settlement>{
        val d=balances.filter{it.net < -0.01}.map{it.memberId to -it.net}.toMutableList()
        val c=balances.filter{it.net > 0.01}.map{it.memberId to it.net}.toMutableList()
        val out=mutableListOf<Settlement>(); var di=0; var ci=0
        while(di<d.size&&ci<c.size){ val a=minOf(d[di].second,c[ci].second); out+=Settlement(d[di].first,c[ci].first,a); d[di]=d[di].copy(second=d[di].second-a); c[ci]=c[ci].copy(second=c[ci].second-a); if(d[di].second<.01)di++; if(c[ci].second<.01)ci++ }
        return out
    }
}
