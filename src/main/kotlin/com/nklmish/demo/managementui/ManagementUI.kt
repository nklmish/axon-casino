package com.nklmish.demo.managementui

import com.nklmish.demo.managementdata.TotalDeposited
import com.nklmish.demo.walletsummary.WalletSummary
import com.nklmish.demo.walletsummary.WalletSummaryCreatedEvt
import com.nklmish.demo.walletsummary.WalletSummaryUpdatedEvt
import com.vaadin.addon.charts.Chart
import com.vaadin.addon.charts.model.*
import com.vaadin.addon.charts.model.style.SolidColor
import com.vaadin.annotations.Push
import com.vaadin.annotations.Theme
import com.vaadin.annotations.Widgetset
import com.vaadin.server.Sizeable
import com.vaadin.server.VaadinRequest
import com.vaadin.spring.annotation.SpringUI
import com.vaadin.ui.HorizontalSplitPanel
import com.vaadin.ui.UI
import com.vaadin.ui.VerticalLayout
import org.springframework.context.annotation.Profile
import java.math.BigDecimal
import java.util.*

@SpringUI(path = "/management")
@Widgetset("AppWidgetset")
@Theme("mytheme")
@Push
@Profile("gui")
class ManagementUI(private val managementDataCollector: ManagementDataCollector) : UI(), ManagementEventListener {
    private val allSeries: MutableMap<String, DataSeries>
    private val availableSeries = ListSeries("Available")
    private val bettedSeries = ListSeries("Betted")
    private val withdrawingSeries = ListSeries("Withdrawing")
    private var allWalletsChart: Chart? = null
    private val USD_TO_EUR = BigDecimal("0.85")

    init {
        allSeries = HashMap()
    }

    override fun init(vaadinRequest: VaadinRequest) {
        for (currency in Arrays.asList("EUR", "USD")) {
            val series = DataSeries()
            series.plotOptions = PlotOptionsSpline()
            series.name = "$currency deposits"
            allSeries[currency] = series
        }
        managementDataCollector.register(this)

        val totalsColumn = VerticalLayout()
        totalsColumn.setSizeFull()
        for (chart in totalDepositedCharts()) totalsColumn.addComponent(chart)

        val walletsColumn = VerticalLayout()
        walletsColumn.setSizeFull()
        this.allWalletsChart = allWalletsChart()
        walletsColumn.addComponent(allWalletsChart)

        val splitPanel = HorizontalSplitPanel()
        splitPanel.setSplitPosition(50f, Sizeable.Unit.PERCENTAGE)
        splitPanel.firstComponent = totalsColumn
        splitPanel.secondComponent = walletsColumn

        content = splitPanel
    }

    override fun close() {
        managementDataCollector.unregister(this)
        super.close()
    }

    override fun updateTotals(now: Long, totals: List<TotalDeposited>) {
        access {
            for ((currency, amount) in totals) {
                val series = allSeries[currency]
                val shift = series!!.size() > 20
                series.add(DataSeriesItem(now, amount), true, shift)
            }
        }
    }

    private fun initWallets(wallets: List<WalletSummary>) {
        val categories = arrayOfNulls<String>(wallets.size)
        val available = arrayOfNulls<Number>(wallets.size)
        val betted = arrayOfNulls<Number>(wallets.size)
        val withdrawing = arrayOfNulls<Number>(wallets.size)
        for (i in wallets.indices) {
            val (walletId, currency, available1, betted1, withdrawing1) = wallets[i]
            categories[i] = walletId
            var multiplier: BigDecimal? = null
            when (currency) {
                "EUR" -> multiplier = BigDecimal.ONE
                "USD" -> multiplier = USD_TO_EUR
            }
            available[i] = available1!!.multiply(multiplier!!)
            betted[i] = betted1!!.multiply(multiplier)
            withdrawing[i] = withdrawing1!!.multiply(multiplier)
        }
        access {
            val configuration = allWalletsChart!!.configuration
            configuration.getxAxis().setCategories(*categories)
            availableSeries.setData(*available)
            bettedSeries.setData(*betted)
            withdrawingSeries.setData(*withdrawing)
            allWalletsChart!!.drawChart(configuration)
        }
    }

    override fun on(evt: WalletSummaryCreatedEvt) {
        val configuration = allWalletsChart!!.configuration
        val (walletId, currency, available, betted, withdrawing) = evt.walletSummary
        configuration.getxAxis().addCategory(walletId)
        var multiplier: BigDecimal? = null
        when (currency) {
            "EUR" -> multiplier = BigDecimal.ONE
            "USD" -> multiplier = USD_TO_EUR
        }
        availableSeries.addData(available!!.multiply(multiplier!!))
        bettedSeries.addData(betted!!.multiply(multiplier))
        withdrawingSeries.addData(withdrawing!!.multiply(multiplier))
        allWalletsChart!!.drawChart()
    }

    override fun on(evt: WalletSummaryUpdatedEvt) {
        val configuration = allWalletsChart!!.configuration
        val (walletId, currency, available, betted, withdrawing) = evt.walletSummary
        for (i in 0 until configuration.getxAxis().categories.size) {
            if (configuration.getxAxis().categories[i] == walletId) {
                var multiplier: BigDecimal? = null
                when (currency) {
                    "EUR" -> multiplier = BigDecimal.ONE
                    "USD" -> multiplier = USD_TO_EUR
                }
                availableSeries.updatePoint(i, available!!.multiply(multiplier!!))
                bettedSeries.updatePoint(i, betted!!.multiply(multiplier))
                withdrawingSeries.updatePoint(i, withdrawing!!.multiply(multiplier))
            }
        }
    }

    private fun totalDepositedCharts(): List<Chart> {
        val charts = ArrayList<Chart>()
        for (currency in allSeries.keys) {
            val chart = Chart()
            val configuration = chart.configuration
            configuration.chart.type = ChartType.SPLINE
            configuration.title.text = "Total deposited $currency"

            val xAxis = configuration.getxAxis()
            xAxis.type = AxisType.DATETIME
            xAxis.tickPixelInterval = 150

            val yAxis = configuration.getyAxis()
            yAxis.title = AxisTitle("Amount")
            yAxis.min = 0

            configuration.tooltip.enabled = false
            configuration.legend.enabled = false
            configuration.addSeries(allSeries[currency])

            chart.setSizeFull()

            charts.add(chart)
        }
        initWallets(managementDataCollector.findWallets())
        return charts
    }

    private fun allWalletsChart(): Chart {
        val chart = Chart(ChartType.BAR)
        chart.setSizeFull()

        val conf = chart.configuration

        conf.setTitle("Wallets")

        val x = XAxis()
        x.setCategories()
        conf.addxAxis(x)

        val y = YAxis()
        y.min = 0
        y.setTitle("Value (converted to EUR)")
        conf.addyAxis(y)

        val legend = Legend()
        legend.backgroundColor = SolidColor("#FFFFFF")
        legend.reversed = true

        val plot = PlotOptionsSeries()
        plot.stacking = Stacking.NORMAL
        conf.setPlotOptions(plot)

        conf.addSeries(availableSeries)
        conf.addSeries(bettedSeries)
        conf.addSeries(withdrawingSeries)

        chart.drawChart(conf)

        return chart
    }

}
