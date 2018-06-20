package com.nklmish.demo

import com.vaadin.annotations.Theme
import com.vaadin.annotations.Widgetset
import com.vaadin.server.ExternalResource
import com.vaadin.server.VaadinRequest
import com.vaadin.spring.annotation.SpringUI
import com.vaadin.ui.Link
import com.vaadin.ui.UI
import com.vaadin.ui.VerticalLayout

@SpringUI
@Widgetset("AppWidgetset")
@Theme("mytheme")
class MainUI : UI() {

    override fun init(vaadinRequest: VaadinRequest) {
        val playerUI = Link("Player UI", ExternalResource("./player"))
        val managementUI = Link("Management UI", ExternalResource("./management"))

        val layout = VerticalLayout()
        layout.addComponents(playerUI, managementUI)

        content = layout
    }
}
