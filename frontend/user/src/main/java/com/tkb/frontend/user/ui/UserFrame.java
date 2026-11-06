package com.tkb.frontend.user.ui;

import com.tkb.frontend.user.net.TcpTkbClient;
import com.tkb.frontend.user.net.TkbClient;

public class UserFrame extends MainFrame {
    public UserFrame() {
        this(new TcpTkbClient());
    }

    public UserFrame(TkbClient client) {
        super(true, client);
    }
}
