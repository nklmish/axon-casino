package com.nklmish.demo.playerui

import com.nklmish.demo.wallet.*
import com.nklmish.demo.walletsummary.SingleWalletSummaryQuery
import com.nklmish.demo.walletsummary.WalletSummary
import com.nklmish.demo.walletsummary.WalletSummaryCreatedEvt
import com.nklmish.demo.walletsummary.WalletSummaryUpdatedEvt
import com.vaadin.addon.charts.Chart
import com.vaadin.addon.charts.model.*
import com.vaadin.addon.charts.model.style.SolidColor
import com.vaadin.annotations.Push
import com.vaadin.annotations.Theme
import com.vaadin.annotations.Widgetset
import com.vaadin.data.provider.ListDataProvider
import com.vaadin.server.DefaultErrorHandler
import com.vaadin.server.Sizeable
import com.vaadin.server.VaadinRequest
import com.vaadin.spring.annotation.SpringUI
import com.vaadin.ui.*
import org.axonframework.commandhandling.gateway.CommandGateway
import org.axonframework.queryhandling.QueryGateway
import org.axonframework.queryhandling.responsetypes.ResponseTypes
import org.springframework.context.annotation.Profile
import java.math.BigDecimal
import java.util.*

@SpringUI(path = "/player")
@Widgetset("AppWidgetset")
@Theme("mytheme")
@Push
@Profile("gui")
class PlayerUI(private val commandGateway: CommandGateway, private val queryGateway: QueryGateway, private val playerEventDistributor: PlayerEventDistributor) : UI(), PlayerEventListener {
    @Volatile
    private var newWalletId: String? = null
    @Volatile
    private var activeWalletId: String? = null
    @Volatile
    private var activeWalletCurrency: String? = null
    private var currentNotification: Notification? = null
    private val availableSeries = ListSeries("Available", 0)
    private val bettedSeries = ListSeries("Betted", 0)
    private val withdrawingSeries = ListSeries("Withdrawing", 0)
    private val bets = ArrayList<BetData>()
    private val betsDataProvider = ListDataProvider(bets)

    override fun on(evt: WalletSummaryCreatedEvt) {
        if (newWalletId != null && newWalletId == evt.walletSummary.walletId) {
            activeWalletId = evt.walletSummary.walletId
            activeWalletCurrency = evt.walletSummary.currency
            availableSeries.updatePoint(0, evt.walletSummary.available)
            bettedSeries.updatePoint(0, evt.walletSummary.betted)
            withdrawingSeries.updatePoint(0, evt.walletSummary.withdrawing)
            access { setContent() }
        }
    }

    override fun on(evt: WalletSummaryUpdatedEvt) {
        access {
            availableSeries.updatePoint(0, evt.walletSummary.available)
            bettedSeries.updatePoint(0, evt.walletSummary.betted)
            withdrawingSeries.updatePoint(0, evt.walletSummary.withdrawing)
        }
    }

    override fun on(evt: WithdrawalApprovedEvent) {
        access {
            if (currentNotification != null) currentNotification!!.close()
            currentNotification = Notification.show("Withdrawal approved.", Notification.Type.WARNING_MESSAGE)
        }
    }

    override fun on(evt: WithdrawalDeniedEvent) {
        access {
            if (currentNotification != null) currentNotification!!.close()
            currentNotification = Notification.show("Withdrawal denied.", Notification.Type.ERROR_MESSAGE)
        }
    }

    override fun on(evt: BetPlacedEvent) {
        access {
            bets.add(0, BetData(evt.betId, evt.amount, null))
            betsDataProvider.refreshAll()
        }
    }

    override fun on(evt: BetWonEvent) {
        access {
            if (currentNotification != null) currentNotification!!.close()
            currentNotification = Notification.show("Bet won!", Notification.Type.WARNING_MESSAGE)
            for (bet in bets) {
                if (bet.betId == evt.betId && bet.amountWon == null) {
                    bet.amountWon = evt.amountPayout
                    break
                }
            }
            betsDataProvider.refreshAll()
        }
    }

    override fun on(evt: BetLostEvent) {
        access {
            if (currentNotification != null) currentNotification!!.close()
            currentNotification = Notification.show("Bet lost.", Notification.Type.WARNING_MESSAGE)
            for (bet in bets) {
                if (bet.betId == evt.betId && bet.amountWon == null) {
                    bet.amountWon = BigDecimal.ZERO
                    break
                }
            }
            betsDataProvider.refreshAll()
        }
    }

    override fun init(vaadinRequest: VaadinRequest) {
        errorHandler = object : DefaultErrorHandler() {
            override fun error(event: com.vaadin.server.ErrorEvent) {
                var cause = event.throwable
                while (cause.cause != null) cause = cause.cause
                Notification.show(cause.message, Notification.Type.ERROR_MESSAGE)
            }
        }
        setContent()
        playerEventDistributor.register(this)
    }

    override fun close() {
        playerEventDistributor.unregister(this)
        super.close()
    }

    private fun setContent() {
        if (activeWalletId == null) {
            val commands = HorizontalLayout()
            commands.addComponents(createWalletPanel(), useExistingWalletPanel())

            val layout = VerticalLayout()
            layout.addComponents(commands)
            layout.setComponentAlignment(commands, Alignment.MIDDLE_CENTER)
            layout.setSizeFull()

            content = layout
        } else {
            val wallet = VerticalLayout()
            wallet.addComponents(
                    walletChart(),
                    depositPanel(),
                    requestWithdrawalPanel(),
                    changeWalletButton()
            )

            val game = VerticalLayout()
            game.addComponents(
                    betPanel(),
                    betGrid()
            )

            val hsplit = HorizontalSplitPanel()
            hsplit.setSizeFull()
            hsplit.firstComponent = wallet
            hsplit.secondComponent = game
            hsplit.setSplitPosition(30f, Sizeable.Unit.PERCENTAGE)

            content = hsplit
        }
    }

    private fun createWalletPanel(): Panel {
        val id = TextField("Wallet Id")
        val currency = TextField("Currency")
        val submit = Button("Submit")

        submit.addClickListener { _ ->
            newWalletId = id.value
            val cmd = CreateWalletCommand(
                    newWalletId!!,
                    currency.value
            )
            commandGateway.sendAndWait<Any>(cmd)
        }

        val form = FormLayout()
        form.setMargin(true)
        form.addComponents(id, currency, submit)

        val panel = Panel("Create new wallet")
        panel.content = form
        return panel
    }

    private fun useExistingWalletPanel(): Panel {
        val id = TextField("Wallet Id")
        val submit = Button("Submit")

        submit.addClickListener { _ ->
            val walletId = id.value
            queryGateway
                    .query(SingleWalletSummaryQuery(walletId), ResponseTypes.instanceOf(WalletSummary::class.java))
                    .whenComplete { walletSummary, throwable ->
                        if (walletSummary != null) {
                            activeWalletId = walletSummary.walletId
                            activeWalletCurrency = walletSummary.currency
                            availableSeries.updatePoint(0, walletSummary.available)
                            bettedSeries.updatePoint(0, walletSummary.betted)
                            withdrawingSeries.updatePoint(0, walletSummary.withdrawing)
                            access { setContent() }
                        } else if (throwable != null) {
                            access { Notification.show("Unable to find wallet $walletId", Notification.Type.ERROR_MESSAGE) }
                        }
                    }
        }

        val form = FormLayout()
        form.setMargin(true)
        form.addComponents(id, submit)

        val panel = Panel("Use existing wallet")
        panel.content = form
        return panel
    }

    private fun walletChart(): Component {
        val chart = Chart(ChartType.BAR)
        chart.setHeight("200px")

        val conf = chart.configuration

        conf.setTitle("Wallet")

        val x = XAxis()
        x.setCategories(activeWalletId!!)
        conf.addxAxis(x)

        val y = YAxis()
        y.min = 0
        y.setTitle(activeWalletCurrency)
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

    private fun depositPanel(): Panel {
        val amount = TextField("Amount")
        val submit = Button("Submit")

        submit.addClickListener { _ ->
            val cmd = DepositCommand(
                    activeWalletId!!,
                    BigDecimal(amount.value)
            )
            commandGateway.sendAndWait<Any>(cmd)
            amount.value = ""
            Notification.show("Money deposited", Notification.Type.HUMANIZED_MESSAGE)
        }

        val form = FormLayout()
        form.setMargin(true)
        form.addComponents(amount, submit)

        val panel = Panel("Make deposit")
        panel.content = form
        return panel
    }

    private fun requestWithdrawalPanel(): Panel {
        val amount = TextField("Amount")
        val submit = Button("Submit")

        submit.addClickListener { _ ->
            val cmd = RequestWithdrawCommand(
                    activeWalletId!!,
                    BigDecimal(amount.value)
            )
            commandGateway.sendAndWait<Any>(cmd)
            amount.value = ""
            currentNotification = Notification.show("Withdrawal requested", Notification.Type.HUMANIZED_MESSAGE)
        }

        val form = FormLayout()
        form.setMargin(true)
        form.addComponents(amount, submit)

        val panel = Panel("Request withdrawal")
        panel.content = form
        return panel
    }

    private fun changeWalletButton(): Button {
        val button = Button("Change wallet")
        button.addClickListener { _ ->
            newWalletId = null
            activeWalletId = null
            activeWalletCurrency = null
            availableSeries.updatePoint(0, 0)
            bettedSeries.updatePoint(0, 0)
            withdrawingSeries.updatePoint(0, 0)
            bets.clear()
            betsDataProvider.refreshAll()
            setContent()
        }
        return button
    }

    private fun betPanel(): Panel {
        val id = TextField("BetId")
        val amount = TextField("Amount")
        val submit = Button("Submit")

        submit.addClickListener { _ ->
            val cmd = PlaceBetCommand(
                    activeWalletId!!,
                    id.value,
                    BigDecimal(amount.value)
            )
            commandGateway.sendAndWait<Any>(cmd)
            id.value = ""
            amount.value = ""
            currentNotification = Notification.show("Bet placed", Notification.Type.HUMANIZED_MESSAGE)
        }

        val form = FormLayout()
        form.setMargin(true)
        form.addComponents(id, amount, submit)

        val panel = Panel("Place a bet")
        panel.content = form
        return panel
    }

    private fun betGrid(): Grid<BetData> {
        val grid = Grid<BetData>()
        grid.setSizeFull()
        grid.dataProvider = betsDataProvider
        grid.addColumn { it.betId }.caption = "BetId"
        grid.addColumn { it.amountBetted }.caption = "Amount betted"
        grid.addColumn { it.amountWon }.caption = "Amount won"
        return grid
    }


}
