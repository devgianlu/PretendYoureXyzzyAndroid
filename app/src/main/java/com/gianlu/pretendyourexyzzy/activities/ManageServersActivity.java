package com.gianlu.pretendyourexyzzy.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.adapters.ServersAdapter;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.dialogs.Dialogs;

public class ManageServersActivity extends ActivityWithDialog implements ServersAdapter.Listener, Dialogs.OnAddServer {
    private RecyclerMessageView rmv;
    private ServersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_manage_servers);
        setTitle(R.string.manageServers);

        rmv = findViewById(R.id.manageServers_list);
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
        if (item.getItemId() == R.id.manageServers_add) {
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
