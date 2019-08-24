package com.gianlu.pretendyourexyzzy.SpareActivities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.CasualViews.RecyclerMessageView;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.pretendyourexyzzy.Adapters.ServersAdapter;
import com.gianlu.pretendyourexyzzy.Dialogs.Dialogs;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.R;

public class ManageServersActivity extends ActivityWithDialog implements ServersAdapter.Listener, Dialogs.OnAddServer {
    private RecyclerMessageView rmv;
    private ServersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        rmv = new RecyclerMessageView(this);
        setContentView(rmv);
        setTitle(R.string.manageServers);

        rmv.disableSwipeRefresh();
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);
        rmv.dividerDecoration(RecyclerView.VERTICAL);
        loadServers();
    }

    @Override
    public void loadServers() {
        adapter = new ServersAdapter(this, Pyx.Server.loadAllServers(), this);
        rmv.loadListData(adapter, false);
    }

    @Override
    public void removeItem(@NonNull Pyx.Server server) {
        if (adapter != null) adapter.removeItem(server);
    }

    @Override
    public void startTests() {
        if (adapter != null) adapter.startTests();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manage_servers, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.manageServers_add:
                showDialog(Dialogs.addServer(this, null, this));
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void shouldUpdateItemCount(int count) {
        if (count == 0) rmv.showInfo(R.string.noServers);
        else rmv.showList();
    }

    @Override
    public void serverSelected(@NonNull Pyx.Server server) {
        if (server.isEditable()) showDialog(Dialogs.addServer(this, server, this));
    }
}
