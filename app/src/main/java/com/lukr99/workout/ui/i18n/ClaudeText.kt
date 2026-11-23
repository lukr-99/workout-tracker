package com.lukr99.workout.ui.i18n

/** Text for Settings, Claude: the rows, the connect form, the code dialog, Home's proposals card and the toasts. */
object ClaudeText : TextCatalog("claude") {
    val connectTitle = msg("Connect to your Claude server", "Připoj se ke svému serveru Claude")
    val connectIntro = msg(
        "The server's address and its setup secret, from whoever set up the server (cloud/README.md).",
        "Adresa serveru a jeho tajný klíč pro nastavení, od toho, kdo server zřídil (cloud/README.md).",
    )
    val serverAddress = msg("Server address", "Adresa serveru")
    val setupSecret = msg("Setup secret", "Tajný klíč nastavení")
    val connect = msg("Connect", "Připojit")
    val connecting = msg("Connecting…", "Připojuji…")

    val proposedMany = plural(
        "Claude proposed {n} thing", "Claude proposed {n} things",
        "Claude navrhl {n} věc", "Claude navrhl {n} věci", "Claude navrhl {n} věcí",
    )
    val proposedTemplate = msg("Claude proposed a template", "Claude navrhl šablonu")
    val proposedExercise = msg("Claude proposed an exercise", "Claude navrhl cvik")
    val reject = msg("Reject", "Odmítnout")
    val accept = msg("Accept", "Přijmout")
    val moreInSettings = msg("{n} more in Settings, Claude.", "Dalších návrhů ({n}) najdeš v Nastavení, Claude.")

    val notConnected = msg("Not connected", "Nepřipojeno")
    val notConnectedHint = msg(
        "Let Claude read your workouts and runs when you allow it, and propose templates. Needs your own Claude server.",
        "Nech Clauda číst tvoje tréninky a běhy, když to povolíš, a navrhovat šablony. Potřebuje tvůj vlastní server Claude.",
    )
    val connected = msg("Connected", "Připojeno")
    val pushOff = msg(". Push is off, so Ember asks when you open it.", ". Push je vypnutý, Ember se proto zeptá při otevření.")
    val disconnect = msg("Disconnect", "Odpojit")
    val asksTitle = msg("Claude asks for your data", "Claude žádá o tvoje data")
    val asksHint = msg(
        "Allow shares your workouts, runs and templates for 30 minutes.",
        "Povolením sdílíš své tréninky, běhy a šablony na 30 minut.",
    )
    val deny = msg("Deny", "Zamítnout")
    val allow = msg("Allow", "Povolit")
    val whenAsksTitle = msg("When Claude asks for your data", "Když Claude žádá o tvoje data")
    val whenAsksHint = msg(
        "Ask me shows Allow and Deny. Shared data is deleted from the server after 30 minutes.",
        "Zeptat se zobrazí Povolit a Zamítnout. Sdílená data se ze serveru smažou po 30 minutách.",
    )
    val modeAsk = msg("Ask me", "Zeptat se")
    val modeAutomatic = msg("Always share", "Vždy sdílet")
    val modeNever = msg("Never", "Nikdy")
    val sharedData = msg("Shared data", "Sdílená data")
    val sharedAt = msg("Shared at {from}, deleted at {until}.", "Sdíleno v {from}, smaže se v {until}.")
    val nothingShared = msg(
        "Nothing is shared now. Share before a chat to skip the request.",
        "Teď se nic nesdílí. Sdílej před chatem, ať se nemusí čekat na žádost.",
    )
    val shareNow = msg("Share now", "Sdílet teď")
    val sharing = msg("Sharing…", "Sdílím…")
    val stopSharing = msg("Stop sharing", "Přestat sdílet")
    val stopSharingHint = msg("Deletes the copy on the server now.", "Hned smaže kopii na serveru.")
    val delete = msg("Delete", "Smazat")
    val connectClaude = msg("Connect Claude", "Připojit Clauda")
    val connectClaudeHint = msg(
        "Shows the address and a one-time code for Claude's Connectors settings.",
        "Ukáže adresu a jednorázový kód pro nastavení konektorů v Claudovi.",
    )
    val getCode = msg("Get code", "Získat kód")
    val proposalsTitle = msg("Claude's proposals", "Návrhy od Clauda")
    val proposalsEmpty = msg(
        "Nothing waits for you. Templates and exercises Claude proposes show here.",
        "Nic na tebe nečeká. Šablony a cviky, které Claude navrhne, se ukážou tady.",
    )
    val templatePrefix = msg("Template: ", "Šablona: ")
    val exercisePrefix = msg("Exercise: ", "Cvik: ")
    val removeClaude = msg("Remove Claude", "Odebrat Clauda")
    val removeHint = msg(
        "Stops Claude reading Ember at once. Remove the connector in Claude too.",
        "Okamžitě zastaví čtení Emberu Claudem. Konektor odeber i v Claudovi.",
    )
    val remove = msg("Remove", "Odebrat")

    val codeIntro = msg(
        "In Claude, open Settings, Connectors, Add custom connector, and paste this address:",
        "V Claudovi otevři Nastavení, Konektory, Přidat vlastní konektor a vlož tuto adresu:",
    )
    val codeSignIn = msg(
        "On the sign-in page that opens, enter this code. It works once, until {time}.",
        "Na přihlašovací stránce, která se otevře, zadej tento kód. Platí jednou, do {time}.",
    )
    val copyAddress = msg("Copy address", "Kopírovat adresu")
    val addressCopied = msg("Address copied.", "Adresa zkopírována.")

    val proposalSets = plural("{n} set", "{n} sets", "{n} série", "{n} série", "{n} sérií")
    val proposalReps = msg("{reps} reps", "{reps} opakování")
    val proposalRest = msg("rest {time}", "odpočinek {time}")
    val proposalSuperset = msg("superset {group}", "superserie {group}")

    val messageConnected = msg("Connected. Now connect Claude.", "Připojeno. Teď připoj Clauda.")
    val messageDisconnected = msg("Ember is no longer connected on this phone.", "Ember už na tomto telefonu není připojený.")
    val messageShared = msg("Shared for 30 minutes.", "Sdíleno na 30 minut.")
    val messageAllowed = msg("Shared with Claude for 30 minutes.", "Sdíleno s Claudem na 30 minut.")
    val messageDeclined = msg("Claude's request was declined.", "Žádost od Clauda byla zamítnuta.")
    val messageShareDeleted = msg("The shared copy is deleted.", "Sdílená kopie je smazaná.")
    val messageAdded = msg("Added \"{title}\".", "Přidáno: \"{title}\".")
    val messageRejected = msg("Rejected \"{title}\".", "Odmítnuto: \"{title}\".")
    val messageNotConnected = msg("Claude was not connected.", "Claude nebyl připojený.")
    val messageRemoved = msg(
        "Claude can no longer read Ember. Remove the connector in Claude too.",
        "Claude už Ember číst nemůže. Konektor odeber i v Claudovi.",
    )
    val somethingWrong = msg("Something went wrong.", "Něco se nepovedlo.")
}
