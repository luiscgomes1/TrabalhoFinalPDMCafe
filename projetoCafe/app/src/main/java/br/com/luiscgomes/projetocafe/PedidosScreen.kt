package br.com.luiscgomes.projetocafe

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
        
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(
    navController: NavController,
    pedidosDAO: PedidosDAO,
    clientesDAO: ClientesDAO,
    produtosDAO: ProdutosDAO
) {
    var pedidos by remember { mutableStateOf(emptyList<Pedido>()) }
    var clientes by remember { mutableStateOf(emptyList<Cliente>()) }
    var produtos by remember { mutableStateOf(emptyList<Produto>()) }
    var itensTemp by remember { mutableStateOf(mutableListOf<ItemPedido>()) }
    var filteredPedidos by remember { mutableStateOf(emptyList<Pedido>()) }
    var isFiltering by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var clienteFiltro by remember { mutableStateOf("") }
    var cpfFiltro by remember { mutableStateOf(TextFieldValue()) }
        
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var dataSelecionada by remember { mutableStateOf(dateFormatter.format(Calendar.getInstance().time)) }
    var clienteSelecionado by remember { mutableStateOf("") }
    var produtoSelecionado by remember { mutableStateOf("") }
    var quantidade by remember { mutableStateOf(TextFieldValue()) }
    var editingPedido by remember { mutableStateOf<Pedido?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmDialogType by remember { mutableStateOf("") }
    var pedidoToBeDeleted by remember { mutableStateOf<Pedido?>(null) }
        
    val context = LocalContext.current
        
    LaunchedEffect(Unit) {
        try {
            pedidos = pedidosDAO.listarPedidos()
            clientes = clientesDAO.listarClientes()
            produtos = produtosDAO.listarProdutos()
            filteredPedidos = pedidos
            clientes = clientes.filter { it.status == "Ativo" }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao carregar os dados", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkIDExists(id: String): Boolean {
        return pedidos.any { it.idPedido == id }
    }

    fun generateNextId(): String {
        var nextId = (pedidos.size + 1).toString()
        while (checkIDExists(nextId)) {
            nextId = (nextId.toInt() + 1).toString()
        }
        return nextId
    }

    val proximoIdPedido = generateNextId()


    fun clearFields() {
        dataSelecionada = dateFormatter.format(Calendar.getInstance().time)
        clienteSelecionado = ""
        produtoSelecionado = ""
        quantidade = TextFieldValue()
        itensTemp.clear()
        editingPedido = null
    }
        
    fun validateFields(): Boolean {
        return dataSelecionada.isNotBlank() && clienteSelecionado.isNotBlank() && itensTemp.isNotEmpty()
    }
        
    fun addOrUpdatePedido() {
        if (!validateFields()) {
            Toast.makeText(context, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show()
            return
        }
        
        showConfirmDialog = true
        confirmDialogType = if (editingPedido == null) "Adicionar" else "Salvar"
    }
        
    fun confirmAddOrUpdatePedido() {
        val pedido = Pedido(
            idPedido = editingPedido?.idPedido ?: proximoIdPedido.toString(),
            data = dataSelecionada,
            cpfCliente = clienteSelecionado,
            itensPedido = itensTemp.toMutableList()
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            if (editingPedido == null) {
                pedidosDAO.inserirPedido(pedido)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Pedido adicionado", Toast.LENGTH_SHORT).show()
                }
            } else {
                pedidosDAO.atualizarPedido(pedido)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Pedido atualizado", Toast.LENGTH_SHORT).show()
                }
            }
            pedidos = pedidosDAO.listarPedidos()
            filteredPedidos = pedidos
            withContext(Dispatchers.Main) {
                clearFields()
            }
        }
        showConfirmDialog = false
        }
        
            fun addItemTemp() {
                val quantidadeInt = quantidade.text.toIntOrNull()
                if (quantidadeInt == null || quantidadeInt <= 0) {
                    Toast.makeText(context, "Quantidade inválida", Toast.LENGTH_SHORT).show()
                    return
                }
        
                val itemPedido = ItemPedido(
                    produtoId = produtos.find { it.id_produto == produtoSelecionado }?.id_produto ?: "",
                    quantidade = quantidadeInt
                )
                itensTemp.add(itemPedido)
                produtoSelecionado = ""
                quantidade = TextFieldValue()
                // Atualiza o estado para forçar a recomposição
                itensTemp = itensTemp.toMutableList()
            }
        
            fun removePedido(pedido: Pedido) {
                pedidoToBeDeleted = pedido
                showConfirmDialog = true
                confirmDialogType = "Excluir"
            }
        
            fun confirmRemovePedido() {
                pedidoToBeDeleted?.let { pedido ->
                    CoroutineScope(Dispatchers.IO).launch {
                        pedidosDAO.deletarPedido(pedido.idPedido)
                        pedidos = pedidosDAO.listarPedidos()
                        filteredPedidos = pedidos
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Pedido excluído", Toast.LENGTH_SHORT).show()
                            if (editingPedido == pedido) {
                                clearFields()
                                editingPedido = null
                            }
                        }
                    }
                }
                showConfirmDialog = false
            }

    fun removeItemTemp(item: ItemPedido) {
        val novaLista = itensTemp.toMutableList() // Cria uma cópia mutável da lista
        novaLista.remove(item)
        itensTemp = novaLista // Atualiza oestado com a nova lista
        Toast.makeText(context, "Item removido", Toast.LENGTH_SHORT).show()
    }


    fun openItensDialog() {
                showDialog = true
            }
        
            fun closeItensDialog() {
                showDialog = false
            }
        
            fun applyFilter() {
                filteredPedidos = if (cpfFiltro.text.isNotEmpty()) {
                    pedidos.filter { it.cpfCliente == cpfFiltro.text }
                } else if (clienteFiltro.isNotEmpty()) {
                    pedidos.filter { it.cpfCliente == clienteFiltro }
                } else {
                    pedidos
                }
                isFiltering = true
                showFilterDialog = false
            }
        
            fun resetFilter() {
                filteredPedidos = pedidos
                isFiltering = false
                clienteFiltro = ""
                cpfFiltro = TextFieldValue()
            }
        
            val listState = rememberLazyListState()
        
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                            }
                        },
                        title = {
                            Text(
                                text = "Café Ouro Negro de Minas",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    )
                },
                content = { paddingValues ->
                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Ícone da Tela de Pedidos",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tela de Pedidos",
                                textAlign = TextAlign.Center
                            )
                        }
        
                        Spacer(modifier = Modifier.height(16.dp))
        
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 16.dp)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Text("ID do Pedido: ${editingPedido?.idPedido ?: proximoIdPedido}")
                                }
        
                                Spacer(modifier = Modifier.height(16.dp))
        
                                DateFieldWithPicker(
                                    selectedDate = dataSelecionada,
                                    onDateSelected = { dataSelecionada = it }
                                )
        
                                Spacer(modifier = Modifier.height(16.dp))
        
                                Text("Escolha o Cliente:", modifier = Modifier.padding(start = 16.dp))
        
                                ClientesComboBox(
                                    clientes = clientes,
                                    clienteSelecionado = clienteSelecionado,
                                    onClienteSelected = { clienteSelecionado = it }
                                )
        
                                Spacer(modifier = Modifier.height(16.dp))
        
                                Text("Escolha o Produto e a Quantidade:", modifier = Modifier.padding(start = 16.dp))
        
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ProdutosComboBox(
                                            produtos = produtos,
                                            produtoSelecionado = produtoSelecionado,
                                            onProdutoSelected = { produtoSelecionado = it }
                                        )
                                    }
        
                                    Spacer(modifier = Modifier.width(16.dp))
        
                                    TextField(
                                        value = quantidade,
                                        onValueChange = {
                                            if (it.text.all { char -> char.isDigit() }) {
                                                quantidade = it
                                            }
                                        },
                                        label = { Text("Quantidade") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
        
                                Spacer(modifier = Modifier.height(16.dp))
        
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = { addItemTemp() },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Adicionar Item"
                                        )
                                        Text("Adicionar Item")
                                    }
                                }
        
                                Spacer(modifier = Modifier.height(16.dp))
        
                                Text(
                                    text = "Itens do Pedido",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                if (itensTemp.isEmpty()) {
                                    Text(
                                        text = "Nenhum item adicionado",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Button(
                                            onClick = { openItensDialog() },
                                        ) {
                                            Text("Ver Itens do Pedido (${itensTemp.size})")
                                        }
                                    }

                                    if (showDialog) {
                                        AlertDialog(
                                            onDismissRequest = { closeItensDialog() },
                                            title = {
                                                Text("Itens atuais do pedido:")
                                            },
                                            text = {
                                                LazyColumn {
                                                    items(itensTemp) { item ->
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            val produtoDescricao = produtos.find { it.id_produto == item.produtoId }?.descricao ?: "Produto não encontrado"
                                                            Text(
                                                                text = "Produto: $produtoDescricao, Quantidade: ${item.quantidade}",
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            IconButton(
                                                                onClick = {
                                                                    removeItemTemp(item)
                                                                }
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Remover Item"
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                Button(onClick = { closeItensDialog() }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Fechar"
                                                    )
                                                    Text("Fechar")
                                                }
                                            }
                                        )
                                    }
                                }
        
                                Spacer(modifier = Modifier.height(16.dp))
        
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = { addOrUpdatePedido() },
                                    ) {
                                        Text(if (editingPedido == null) "Adicionar Pedido" else "Salvar Edição")
                                    }
                                    Button(
                                        onClick = {
                                            clearFields()
                                        },
                                    ) {
                                        Text(if (editingPedido == null) "Limpar Campos" else "Cancelar Edição")
                                    }
                                }
        
                                Spacer(modifier = Modifier.height(16.dp))
                            }
        
                            item {
                                Text(
                                    text = "Lista de Pedidos",
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
        
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = { showFilterDialog = true },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Filtrar Pedidos"
                                        )
                                        Text("Filtrar")
                                    }
                                    Button(
                                        onClick = { resetFilter() },
                                        enabled = isFiltering
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Limpar Filtros"
                                        )
                                        Text("Todos")
                                    }
                                }
        
                                Spacer(modifier = Modifier.height(16.dp))
                            }
        
                            items(filteredPedidos) { pedido ->
                                Card(
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text("ID do Pedido: ${pedido.idPedido}")
                                        Text("Data: ${pedido.data}")
                                        Text("CPF do Cliente: ${pedido.cpfCliente}")
                                        Text("Itens do Pedido: ${pedido.itensPedido.size}")
        
                                        Spacer(modifier = Modifier.height(8.dp))
        
                                        // Botões de ações para editar e excluir
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(onClick = {
                                                // Popula os campos com os dados do pedido para edição
                                                dataSelecionada = pedido.data
                                                clienteSelecionado = pedido.cpfCliente
                                                itensTemp = pedido.itensPedido.toMutableList()
                                                editingPedido = pedido
                                                // Focar no formulário
                                                CoroutineScope(Dispatchers.Main).launch {
                                                    listState.scrollToItem(0)
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Editar Pedido"
                                                )
                                                Text("Editar")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(onClick = { removePedido(pedido) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Excluir Pedido"
                                                )
                                                Text("Excluir")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        
            if (showFilterDialog) {
                AlertDialog(
                    onDismissRequest = { showFilterDialog = false },
                    title = { Text("Filtrar Pedidos:") },
                    text = {
                        Column {
                            ClientesComboBox(
                                clientes = clientes,
                                clienteSelecionado = clienteFiltro,
                                onClienteSelected = { clienteFiltro = it }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextField(
                                value = cpfFiltro,
                                onValueChange = { cpfFiltro = it },
                                label = { Text("CPF do Cliente") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = { applyFilter() }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Filtrar"

                            )
                            Text("Filtrar")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showFilterDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar"
                            )
                            Text("Cancelar")
                        }
                    }
                )
            }
        
            if (showConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showConfirmDialog = false },
                    title = { Text("$confirmDialogType Pedido") },
                    text = {
                        Text("Você tem certeza que deseja $confirmDialogType este pedido?")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                when (confirmDialogType) {
                                    "Adicionar", "Salvar" -> confirmAddOrUpdatePedido()
                                    "Excluir" -> confirmRemovePedido()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Confirmar"
                            )
                            Text("Confirmar")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showConfirmDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar"
                            )
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
        
        @Composable
        fun ClientesComboBox(
            clientes: List<Cliente>,
            clienteSelecionado: String,
            onClienteSelected: (String) -> Unit
        ) {
            var estadoExpansao by remember { mutableStateOf(false) }
            var estadoCombobox by remember { mutableStateOf(
                clientes.find { it.cpf == clienteSelecionado }?.nome ?: "Selecione um cliente"
            ) }
        
            if (clienteSelecionado.isEmpty()) {
                estadoCombobox = "Selecione um cliente"
            }
        
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = estadoCombobox,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { estadoExpansao = !estadoExpansao }
                        .padding(16.dp)
                )
        
                DropdownMenu(
                    expanded = estadoExpansao,
                    onDismissRequest = { estadoExpansao = false }
                ) {
                    clientes.forEach { cliente ->
                        DropdownMenuItem(
                            onClick = {
                                onClienteSelected(cliente.cpf)
                                estadoCombobox = cliente.nome
                                estadoExpansao = false
                            },
                            text = { Text(text = cliente.nome) }
                        )
                    }
                }
            }
        }
        
        @Composable
        fun ProdutosComboBox(
            produtos: List<Produto>,
            produtoSelecionado: String,
            onProdutoSelected: (String) -> Unit
        ) {
            var estadoExpansao by remember { mutableStateOf(false) }
            var estadoCombobox by remember { mutableStateOf(
                produtos.find { it.id_produto == produtoSelecionado }?.descricao ?: "Selecione um produto"
            ) }
        
            if (produtoSelecionado.isEmpty()) {
                estadoCombobox = "Selecione um produto"
            }
        
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = estadoCombobox,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { estadoExpansao = !estadoExpansao }
                        .padding(16.dp)
                )
        
                DropdownMenu(
                    expanded = estadoExpansao,
                    onDismissRequest = { estadoExpansao = false }
                ) {
                    produtos.forEach { produto ->
                        DropdownMenuItem(
                            onClick = {
                                onProdutoSelected(produto.id_produto)
                                estadoCombobox = produto.descricao
                                estadoExpansao = false
                            },
                            text = { Text(text = produto.descricao) }
                        )
                    }
                }
            }
        }
        
        @Composable
        fun DateFieldWithPicker(
            selectedDate: String,
            onDateSelected: (String) -> Unit
        ) {
            val context = LocalContext.current
            val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
            val date = remember(selectedDate) {
                val calendar = Calendar.getInstance()
                calendar.time = dateFormatter.parse(selectedDate) ?: Date()
                calendar
            }
            var showDatePicker by remember { mutableStateOf(false) }
            val onDateSet = remember { DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                val newDate = Calendar.getInstance()
                newDate.set(year, monthOfYear, dayOfMonth)
                onDateSelected(dateFormatter.format(newDate.time))
                showDatePicker = false
            } }
        
            Column {
                Text(
                    text = "Escolha a data do pedido:",
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = selectedDate,
                    modifier = Modifier
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 16.dp)
                )
                if (showDatePicker) {
                    DatePickerDialog(
                        context,
                        onDateSet,
                        date.get(Calendar.YEAR),
                        date.get(Calendar.MONTH),
                        date.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            }
        }
