package com.nklmish.demo.playerui

import com.nklmish.demo.wallet.*
import com.nklmish.demo.walletsummary.SingleWalletSummaryQuery
import com.nklmish.demo.walletsummary.WalletSummary
import com.nklmish.demo.walletsummary.WalletSummaryUpdate
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
import org.axonframework.commandhandling.model.ConcurrencyException
import org.axonframework.queryhandling.QueryGateway
import org.axonframework.queryhandling.SubscriptionQueryResult
import org.axonframework.queryhandling.responsetypes.ResponseTypes
import org.springframework.context.annotation.Profile
import java.math.BigDecimal
import java.util.*

@SpringUI(path = "/player")
@Widgetset("AppWidgetset")
@Theme("mytheme")
@Push
@Profile("gui")
class PlayerUI(private val commandGateway: CommandGateway, private val queryGateway: QueryGateway) : UI() {
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

    private var walletSummaryQueryResult: SubscriptionQueryResult<WalletSummary, WalletSummaryUpdate>? = null

    private fun updateWalletSummary(walletSummaryUpdate: WalletSummaryUpdate) {
        val walletSummary = walletSummaryUpdate.walletSummary
        val event = walletSummaryUpdate.event
        if(activeWalletId == null) {
            activeWalletId = walletSummary.walletId
            activeWalletCurrency = walletSummary.currency
            availableSeries.updatePoint(0, walletSummary.available)
            bettedSeries.updatePoint(0, walletSummary.betted)
            withdrawingSeries.updatePoint(0, walletSummary.withdrawing)
            access { setContent() }
        } else {
            access {
                availableSeries.updatePoint(0, walletSummary.available)
                bettedSeries.updatePoint(0, walletSummary.betted)
                withdrawingSeries.updatePoint(0, walletSummary.withdrawing)
                when(event) {
                    is WithdrawalApprovedEvent -> {
                        currentNotification?.close()
                        currentNotification = Notification.show("Withdrawal approved.", Notification.Type.WARNING_MESSAGE)
                    }
                    is WithdrawalDeniedEvent -> {
                        currentNotification?.close()
                        currentNotification = Notification.show("Withdrawal denied.", Notification.Type.ERROR_MESSAGE)
                    }
                    is BetPlacedEvent -> {
                        bets.add(0, BetData(event.betId, event.amount, null))
                        betsDataProvider.refreshAll()
                    }
                    is BetWonEvent -> {
                        currentNotification?.close()
                        currentNotification = Notification.show("Bet won!", Notification.Type.WARNING_MESSAGE)
                        for (bet in bets) {
                            if (bet.betId == event.betId && bet.amountWon == null) {
                                bet.amountWon = event.amountPayout
                                break
                            }
                        }
                        betsDataProvider.refreshAll()
                    }
                    is BetLostEvent -> {
                        currentNotification?.close()
                        currentNotification = Notification.show("Bet lost.", Notification.Type.WARNING_MESSAGE)
                        for (bet in bets) {
                            if (bet.betId == event.betId && bet.amountWon == null) {
                                bet.amountWon = BigDecimal.ZERO
                                break
                            }
                        }
                        betsDataProvider.refreshAll()
                    }
                }
            }
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
    }

    override fun close() {
        walletSummaryQueryResult?.cancel()
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
            val queryResult = queryGateway.subscriptionQuery(SingleWalletSummaryQuery(cmd.walletId),
                    ResponseTypes.instanceOf(WalletSummary::class.java),
                    ResponseTypes.instanceOf(WalletSummaryUpdate::class.java))
            try {
                commandGateway.sendAndWait<Any>(cmd)
                queryResult.updates().subscribe(::updateWalletSummary)
                walletSummaryQueryResult = queryResult
            } catch (e: ConcurrencyException) {
                queryResult.cancel()
                throw RuntimeException("A wallet with this id already exists.")
            } catch (e: Exception) {
                queryResult.cancel()
                throw e
            }
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
            val queryResult = queryGateway.subscriptionQuery(SingleWalletSummaryQuery(walletId),
                    ResponseTypes.instanceOf(WalletSummary::class.java),
                    ResponseTypes.instanceOf(WalletSummaryUpdate::class.java))
            try {
                val summary = queryResult.initialResult().block()
                walletSummaryQueryResult = queryResult
                updateWalletSummary(WalletSummaryUpdate(summary!!, null))
                queryResult.updates().subscribe(::updateWalletSummary)
            } catch(e: NullPointerException) {
                queryResult.cancel()
                access { Notification.show("Unable to find wallet $walletId", Notification.Type.ERROR_MESSAGE) }
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
