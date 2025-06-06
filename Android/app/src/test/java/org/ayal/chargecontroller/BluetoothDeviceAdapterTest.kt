package org.ayal.chargecontroller

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.ayal.chargecontroller.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.MockedStatic
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
// import org.mockito.Mockito.mock // Not strictly needed if not directly calling Mockito.mock()
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class BluetoothDeviceAdapterTest {

    // Mocks for getItemCount
    @Mock private lateinit var mockBluetoothDevice1: BluetoothDevice
    @Mock private lateinit var mockBluetoothDevice2: BluetoothDevice
    @Mock private lateinit var mockBluetoothDevice3: BluetoothDevice

    // Mocks for onBindViewHolder
    @Mock private lateinit var mockContext: Context
    @Mock private lateinit var mockPackageManager: PackageManager
    @Mock private lateinit var mockView: View // Mock for general view interactions, e.g. ViewHolder's itemView for onBind
    @Mock private lateinit var mockDeviceNameTextView: TextView
    @Mock private lateinit var mockDeviceAddressTextView: TextView
    @Mock private lateinit var mockBluetoothDevice: BluetoothDevice // Single device for onBindViewHolder tests
    @Mock private lateinit var mockOnItemClicked: (BluetoothDevice) -> Unit
    @Mock private lateinit var mockViewHolder: BluetoothDeviceAdapter.ViewHolder // Mock for onBindViewHolder

    // Mocks for onCreateViewHolder
    @Mock private lateinit var mockViewGroup: ViewGroup
    @Mock private lateinit var mockLayoutInflater: LayoutInflater
    @Mock private lateinit var mockInflatedView: View // This is the view returned by inflater

    private lateinit var layoutInflaterMockedStatic: MockedStatic<LayoutInflater> // Changed to lateinit

    @Before
    fun setUp() {
        // Common mocks for onBindViewHolder
        `when`(mockViewHolder.itemView).thenReturn(mockView)
        `when`(mockView.context).thenReturn(mockContext) // General context for mockView
        `when`(mockContext.packageManager).thenReturn(mockPackageManager)

        // Since mockViewHolder is a direct mock, we stub its properties directly for onBindViewHolder tests.
        `when`(mockViewHolder.deviceNameTextView).thenReturn(mockDeviceNameTextView)
        `when`(mockViewHolder.deviceAddressTextView).thenReturn(mockDeviceAddressTextView)

        // Setup for onCreateViewHolder
        `when`(mockViewGroup.context).thenReturn(mockContext) // Context for LayoutInflater.from

        // Static mock for LayoutInflater
        layoutInflaterMockedStatic = Mockito.mockStatic(LayoutInflater::class.java)
        // Corrected line: removed ?. and assuming non-null layoutInflaterMockedStatic
        layoutInflaterMockedStatic.`when`<LayoutInflater> { LayoutInflater.from(mockContext) }.thenReturn(mockLayoutInflater)


        `when`(mockLayoutInflater.inflate(R.layout.item_bluetooth_device, mockViewGroup, false)).thenReturn(mockInflatedView)
        `when`(mockInflatedView.findViewById<TextView>(R.id.deviceNameTextView)).thenReturn(mockDeviceNameTextView)
        `when`(mockInflatedView.findViewById<TextView>(R.id.deviceAddressTextView)).thenReturn(mockDeviceAddressTextView)
    }

    @After
    fun tearDown() {
        // Check if initialized before closing, matching lateinit behavior
        if (::layoutInflaterMockedStatic.isInitialized) {
            layoutInflaterMockedStatic.close()
        }
    }

    @Test
    fun onCreateViewHolder_inflatesViewAndReturnsViewHolder() {
        // Arrange
        val devices = emptyList<BluetoothDevice>() // Not used by onCreateViewHolder itself
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)

        // Act
        val createdViewHolder = adapter.onCreateViewHolder(mockViewGroup, 0) // ViewType is 0

        // Assert
        verify(mockLayoutInflater).inflate(R.layout.item_bluetooth_device, mockViewGroup, false)
        assertNotNull(createdViewHolder)
        assertEquals(mockInflatedView, createdViewHolder.itemView)
        assertEquals(mockDeviceNameTextView, createdViewHolder.deviceNameTextView)
        assertEquals(mockDeviceAddressTextView, createdViewHolder.deviceAddressTextView)
    }

    @Test
    fun getItemCount_returnsCorrectSize() {
        val devices = listOf(mockBluetoothDevice1, mockBluetoothDevice2, mockBluetoothDevice3)
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)
        assertEquals(devices.size, adapter.itemCount)
    }

    @Test
    fun onBindViewHolder_permissionGranted_setsNameAndAddress() {
        // Arrange
        val deviceName = "Test Device"
        val deviceAddress = "00:11:22:33:44:55"
        val devices = listOf(mockBluetoothDevice)
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)

        `when`(mockContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)).thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(mockBluetoothDevice.name).thenReturn(deviceName)
        `when`(mockBluetoothDevice.address).thenReturn(deviceAddress)
        `when`(mockViewHolder.deviceNameTextView).thenReturn(mockDeviceNameTextView)
        `when`(mockViewHolder.deviceAddressTextView).thenReturn(mockDeviceAddressTextView)

        // Act
        adapter.onBindViewHolder(mockViewHolder, 0)

        // Assert
        verify(mockDeviceNameTextView).text = deviceName
        verify(mockDeviceAddressTextView).text = deviceAddress
        verify(mockViewHolder.itemView).setOnClickListener(any(View.OnClickListener::class.java))
    }

    @Test
    fun onBindViewHolder_permissionDeniedApiSPlus_setsPermissionDeniedText() {
        // Arrange
        val deviceAddress = "00:11:22:AA:BB:CC"
        val devices = listOf(mockBluetoothDevice)
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)

        `when`(mockContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)).thenReturn(PackageManager.PERMISSION_DENIED)
        `when`(mockBluetoothDevice.address).thenReturn(deviceAddress)

        // Act
        adapter.onBindViewHolder(mockViewHolder, 0)

        // Assert
        // This assertion relies on Build.VERSION.SDK_INT being >= S in the test environment
        // or the SUT's logic handling this appropriately.
        verify(mockDeviceNameTextView).text = "Permission Denied"
        verify(mockDeviceAddressTextView).text = deviceAddress
        verify(mockViewHolder.itemView).setOnClickListener(any(View.OnClickListener::class.java))
    }

    @Test
    fun onBindViewHolder_nullDeviceName_setsUnknownDeviceText() {
        // Arrange
        val deviceAddress = "00:11:22:CC:DD:EE"
        val devices = listOf(mockBluetoothDevice)
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)

        `when`(mockContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)).thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(mockBluetoothDevice.name).thenReturn(null)
        `when`(mockBluetoothDevice.address).thenReturn(deviceAddress)

        // Act
        adapter.onBindViewHolder(mockViewHolder, 0)

        // Assert
        verify(mockDeviceNameTextView).text = "Unknown Device"
        verify(mockDeviceAddressTextView).text = deviceAddress
        verify(mockViewHolder.itemView).setOnClickListener(any(View.OnClickListener::class.java))
    }

    @Test
    fun onBindViewHolder_itemClick_invokesCallback() {
        // Arrange
        val devices = listOf(mockBluetoothDevice)
        val adapter = BluetoothDeviceAdapter(devices, mockOnItemClicked)
        val listenerCaptor = ArgumentCaptor.forClass(View.OnClickListener::class.java)

        `when`(mockContext.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)).thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(mockBluetoothDevice.name).thenReturn("Test Device")
        `when`(mockBluetoothDevice.address).thenReturn("00:11:22:DD:EE:FF")

        // Act
        adapter.onBindViewHolder(mockViewHolder, 0)
        verify(mockViewHolder.itemView).setOnClickListener(listenerCaptor.capture())
        listenerCaptor.value.onClick(mockView) // Simulate click

        // Assert
        verify(mockOnItemClicked).invoke(mockBluetoothDevice)
    }
}
