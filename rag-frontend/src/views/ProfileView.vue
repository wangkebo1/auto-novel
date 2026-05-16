<template>
  <div class="profile-page">
    <div class="profile-hero">
      <div class="hero-main">
        <div>
          <p class="eyebrow">&#36134;&#25143;</p>
          <h1>&#20010;&#20154;&#20013;&#24515;</h1>
          <p class="sub">&#28857;&#25968;&#20313;&#39069;&#12289;&#35746;&#21333;&#12289;&#36864;&#27454;&#21644;&#27969;&#27700;&#37117;&#22312;&#36825;&#37324;&#31649;&#29702;&#12290;</p>
        </div>
        <div class="balance-card">
          <p class="label">&#24403;&#21069;&#28857;&#25968;</p>
          <p class="value">{{ profile?.points ?? 0 }}</p>
          <p class="hint">{{ displayName }}</p>
        </div>
      </div>
    </div>

    <el-row :gutter="16" class="section-grid">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="panel-card">
          <template #header>
            <div class="panel-header">
              <div>
                <h3>&#20805;&#20540;&#22871;&#39184;</h3>
                <p>&#21019;&#24314;&#35746;&#21333;&#21518;&#21487;&#30452;&#25509;&#27169;&#25311;&#25903;&#20184;&#20837;&#36134;&#12290;</p>
              </div>
              <el-button :loading="loading" @click="loadAll">&#21047;&#26032;</el-button>
            </div>
          </template>

          <div v-if="packages.length" class="package-list">
            <div v-for="pkg in packages" :key="pkg.code" class="package-card">
              <div>
                <h4>{{ decodeText(pkg.name) }}</h4>
                <p>{{ pkg.points }} &#28857;</p>
              </div>
              <div class="package-actions">
                <span class="price">{{ formatAmount(pkg.amountCents) }}</span>
                <el-button type="primary" :loading="orderLoadingCode === pkg.code" @click="createAndPayOrder(pkg.code)">
                  &#31435;&#21363;&#20805;&#20540;
                </el-button>
              </div>
            </div>
          </div>
          <el-empty v-else :description="decodeText('&#26242;&#26080;&#22871;&#39184;&#25968;&#25454;')" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="panel-card">
          <template #header>
            <div class="panel-header">
              <div>
                <h3>&#36134;&#21495;&#20449;&#24687;</h3>
                <p>&#26597;&#30475;&#24403;&#21069;&#30331;&#24405;&#29992;&#25143;&#30340;&#22522;&#26412;&#36134;&#21153;&#20449;&#24687;&#12290;</p>
              </div>
            </div>
          </template>

          <div class="profile-meta">
            <div class="meta-item">
              <span class="meta-label">&#29992;&#25143;&#21517;</span>
              <span class="meta-value">{{ profile?.username || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">&#26165;&#31216;</span>
              <span class="meta-value">{{ profile?.nickname || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">Email</span>
              <span class="meta-value">{{ profile?.email || '-' }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">&#35282;&#33394;</span>
              <span class="meta-value">{{ profile?.roles || '-' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" class="panel-card">
      <template #header>
        <div class="panel-header">
          <div>
            <h3>&#35746;&#21333;&#35760;&#24405;</h3>
            <p>&#21487;&#20197;&#26597;&#30475;&#20805;&#20540;&#35746;&#21333;&#24182;&#21457;&#36215;&#36864;&#27454;&#30003;&#35831;&#12290;</p>
          </div>
        </div>
      </template>

      <el-table :data="orders" stripe>
        <el-table-column prop="orderNo" label="&#35746;&#21333;&#21495;" min-width="180" />
        <el-table-column prop="packageName" label="&#22871;&#39184;" min-width="140">
          <template #default="scope">{{ decodeText(scope.row.packageName) }}</template>
        </el-table-column>
        <el-table-column prop="points" label="&#28857;&#25968;" width="100" />
        <el-table-column label="&#37329;&#39069;" width="120">
          <template #default="scope">{{ formatAmount(scope.row.amountCents) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="&#29366;&#24577;" width="120">
          <template #default="scope">
            <el-tag :type="orderTagType(scope.row.status)">{{ orderStatusText(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="&#21019;&#24314;&#26102;&#38388;" min-width="180">
          <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="&#25805;&#20316;" width="180">
          <template #default="scope">
            <el-button
              v-if="scope.row.status === 'PENDING'"
              type="primary"
              link
              @click="payExistingOrder(scope.row.id)"
            >
              &#27169;&#25311;&#25903;&#20184;
            </el-button>
            <el-button
              v-if="scope.row.refundable"
              type="danger"
              link
              @click="openRefundDialog(scope.row)"
            >
              &#30003;&#35831;&#36864;&#27454;
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-row :gutter="16" class="section-grid bottom-grid">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="panel-card">
          <template #header>
            <div class="panel-header">
              <div>
                <h3>&#36864;&#27454;&#35760;&#24405;</h3>
                <p>&#26597;&#30475;&#25152;&#26377;&#36864;&#27454;&#30003;&#35831;&#30340;&#22788;&#29702;&#29366;&#24577;&#12290;</p>
              </div>
            </div>
          </template>

          <el-timeline v-if="refunds.length">
            <el-timeline-item
              v-for="refund in refunds"
              :key="refund.id"
              :timestamp="formatTime(refund.createdAt)"
              :type="refundTimelineType(refund.status)"
            >
              <div class="timeline-title">{{ refundStatusText(refund.status) }}</div>
              <div class="timeline-desc">{{ refund.reason || '-' }}</div>
              <div class="timeline-desc" v-if="refund.reviewerNote">{{ refund.reviewerNote }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else :description="decodeText('&#26242;&#26080;&#36864;&#27454;&#35760;&#24405;')" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="hover" class="panel-card">
          <template #header>
            <div class="panel-header">
              <div>
                <h3>&#28857;&#25968;&#27969;&#27700;</h3>
                <p>&#26368;&#36817; 20 &#26465;&#28857;&#25968;&#21464;&#21160;&#35760;&#24405;&#12290;</p>
              </div>
            </div>
          </template>

          <el-timeline v-if="transactions.length">
            <el-timeline-item
              v-for="tx in transactions"
              :key="tx.id"
              :timestamp="formatTime(tx.createdAt)"
              :type="tx.changeAmount >= 0 ? 'success' : 'danger'"
            >
              <div class="timeline-title">{{ tx.description || tx.sourceType }}</div>
              <div class="timeline-desc">
                <span :class="tx.changeAmount >= 0 ? 'income' : 'expense'">
                  {{ tx.changeAmount > 0 ? '+' : '' }}{{ tx.changeAmount }}
                </span>
                <span class="timeline-gap">/</span>
                <span>&#20313;&#39069; {{ tx.balanceAfter }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else :description="decodeText('&#26242;&#26080;&#28857;&#25968;&#27969;&#27700;')" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="refundDialogVisible" title="&#30003;&#35831;&#36864;&#27454;" width="480px">
      <el-form label-position="top">
        <el-form-item label="&#36864;&#27454;&#21407;&#22240;">
          <el-input
            v-model="refundReason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            :placeholder="decodeText('&#35831;&#22635;&#20889;&#36864;&#27454;&#21407;&#22240;')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundDialogVisible = false">&#21462;&#28040;</el-button>
        <el-button type="primary" :loading="refundSubmitting" @click="submitRefund">
          &#25552;&#20132;&#30003;&#35831;
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { profileApi } from '@/api'
import type { PaymentOrder, PaymentPackage, PointTransaction, ProfileResponse, RefundOrder } from '@/api'

const loading = ref(false)
const orderLoadingCode = ref('')
const refundSubmitting = ref(false)
const refundDialogVisible = ref(false)
const refundReason = ref('')
const refundOrderId = ref<number | null>(null)

const profile = ref<ProfileResponse | null>(null)
const packages = ref<PaymentPackage[]>([])
const orders = ref<PaymentOrder[]>([])
const refunds = ref<RefundOrder[]>([])
const transactions = ref<PointTransaction[]>([])

const displayName = computed(() => profile.value?.nickname || profile.value?.username || '-')

const textDecoder = document.createElement('textarea')
function decodeText(value?: string | null) {
  if (!value) {
    return '-'
  }
  textDecoder.innerHTML = value
  return textDecoder.value
}

function formatAmount(amountCents: number) {
  return (amountCents / 100).toFixed(2) + ' 元'
}

function formatTime(value?: string | null) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ')
}

function orderStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '待支付',
    PAID: '已支付',
    REFUNDED: '已退款',
    CLOSED: '已关闭',
  }
  return map[status] || status
}

function orderTagType(status: string) {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    PENDING: 'warning',
    PAID: 'success',
    REFUNDED: 'info',
    CLOSED: 'danger',
  }
  return map[status] || 'info'
}

function refundStatusText(status: string) {
  const map: Record<string, string> = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
  }
  return map[status] || status
}

function refundTimelineType(status: string) {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'danger' | 'info'> = {
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
  }
  return map[status] || 'info'
}

async function loadAll() {
  loading.value = true
  try {
    const [profileRes, packagesRes, ordersRes, refundsRes, txRes] = await Promise.all([
      profileApi.getProfile(),
      profileApi.listPackages(),
      profileApi.listOrders(),
      profileApi.listRefunds(),
      profileApi.listPointTransactions(),
    ])
    profile.value = profileRes.data
    packages.value = packagesRes.data
    orders.value = ordersRes.data
    refunds.value = refundsRes.data
    transactions.value = txRes.data
  } finally {
    loading.value = false
  }
}

async function createAndPayOrder(packageCode: string) {
  orderLoadingCode.value = packageCode
  try {
    const orderRes = await profileApi.createOrder({ packageCode })
    await profileApi.payOrder(orderRes.data.id)
    ElMessage.success('充值成功，点数已到账')
    await loadAll()
  } finally {
    orderLoadingCode.value = ''
  }
}

async function payExistingOrder(orderId: number) {
  await profileApi.payOrder(orderId)
  ElMessage.success('订单已支付')
  await loadAll()
}

function openRefundDialog(order: PaymentOrder) {
  refundOrderId.value = order.id
  refundReason.value = ''
  refundDialogVisible.value = true
}

async function submitRefund() {
  if (!refundOrderId.value) {
    return
  }
  if (!refundReason.value.trim()) {
    ElMessage.warning('请先填写退款原因')
    return
  }
  refundSubmitting.value = true
  try {
    await profileApi.requestRefund(refundOrderId.value, { reason: refundReason.value.trim() })
    ElMessage.success('退款申请已提交')
    refundDialogVisible.value = false
    refundReason.value = ''
    refundOrderId.value = null
    await loadAll()
  } finally {
    refundSubmitting.value = false
  }
}

onMounted(() => {
  void loadAll()
})
</script>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.profile-hero {
  background: linear-gradient(135deg, #0f172a 0%, #0f766e 60%, #99f6e4 100%);
  padding: 32px;
  border-radius: 20px;
  color: #f8fafc;
}

.hero-main {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-end;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.24em;
  text-transform: uppercase;
}

h1 {
  margin: 0 0 10px;
  font-size: 30px;
  font-weight: 700;
}

.sub {
  margin: 0;
  color: #dbeafe;
}

.balance-card {
  min-width: 220px;
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.2);
  text-align: right;
}

.label {
  margin: 0;
  font-size: 12px;
  color: #dbeafe;
}

.value {
  margin: 6px 0;
  font-size: 34px;
  font-weight: 700;
}

.hint {
  margin: 0;
  font-size: 12px;
  color: #dbeafe;
}

.section-grid,
.bottom-grid {
  margin: 0 !important;
}

.panel-card {
  border-radius: 18px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.panel-header h3 {
  margin: 0 0 4px;
  font-size: 18px;
}

.panel-header p {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.package-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.package-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: #f8fafc;
}

.package-card h4 {
  margin: 0 0 6px;
  font-size: 16px;
}

.package-card p {
  margin: 0;
  color: #6b7280;
}

.package-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.price {
  font-size: 20px;
  font-weight: 700;
  color: #0f766e;
}

.profile-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.meta-item {
  padding: 14px 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
}

.meta-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  color: #6b7280;
}

.meta-value {
  font-size: 15px;
  color: #111827;
  word-break: break-all;
}

.timeline-title {
  font-weight: 600;
  color: #111827;
}

.timeline-desc {
  margin-top: 4px;
  color: #6b7280;
  word-break: break-word;
}

.timeline-gap {
  margin: 0 8px;
  color: #9ca3af;
}

.income {
  color: #059669;
  font-weight: 600;
}

.expense {
  color: #dc2626;
  font-weight: 600;
}

@media (max-width: 900px) {
  .hero-main {
    flex-direction: column;
    align-items: flex-start;
  }

  .balance-card {
    width: 100%;
    text-align: left;
  }

  .profile-meta {
    grid-template-columns: 1fr;
  }

  .package-card {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
